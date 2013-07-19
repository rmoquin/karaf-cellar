/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.hazelcast;

import java.io.IOException;
import java.util.*;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

/**
 * A group manager implementation powered by Hazelcast.
 * The role of this class is to provide means of creating groups, setting nodes to groups etc.
 * Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager, EntryListener {
    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);
    private NodeConfiguration nodeConfiguration;
    private List<GroupConfiguration> groupMemberships;
    private final Map<String, String> pidGroupNameMap = new HashMap<String, String>();
    private final Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private final Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private final Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private final Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private EventTransportFactory eventTransportFactory;
    private CellarCluster masterCluster;
    private ConfigurationAdmin configurationAdmin;

    public void init() {
        IMap groupConfiguration = (IMap) masterCluster.getMap(Configurations.GROUP_CONFIGURATION_DO_STORE);
        groupConfiguration.addEntryListener(this, true);
    }

    public void destroy() {
        Node local = this.getNode();
        Set<Group> groups = this.listGroups(local);
        for (Group group : groups) {
            group.getNodes().remove(local);
        }
        // shutdown the group consumer/producers
        for (Map.Entry<String, EventConsumer> consumerEntry : groupConsumer.entrySet()) {
            EventConsumer consumer = consumerEntry.getValue();
            consumer.stop();
        }
        groupConsumer.clear();
        groupProducers.clear();
    }

    @Override
    public String getPidForGroup(String groupName) {
        return this.getPidForGroup(groupName);
    }

    @Override
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    public void nodeMembershipsReceived(NodeConfiguration nodeConfiguration) {
        LOGGER.warn("A NODE MEMBERSHIP CONFIGURATION WAS REGISTERED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        this.nodeConfiguration = nodeConfiguration;
    }

    public void nodeMembershipsRemoved(NodeConfiguration nodeConfiguration) {
        LOGGER.warn("A NODE MEMBERSHIP CONFIGURATION WAS REMOVED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        this.nodeConfiguration = null;
    }

    public void groupConfigured(GroupConfiguration group, Map<String, Object> properties) {
        LOGGER.warn("A NEW GROUP WAS REGISTERED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + properties.get(org.osgi.framework.Constants.SERVICE_PID));
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        pidGroupNameMap.put(servicePid, group.getGroupName());
        registerGroup(group.getGroupName());
    }

    public void groupRemoved(GroupConfiguration group, Map<String, Object> properties) {
        LOGGER.warn("A GROUP Configuration was removed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + properties.get(org.osgi.framework.Constants.SERVICE_PID));
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        pidGroupNameMap.remove(servicePid);
        if (this.nodeConfiguration.getGroupNames().contains(group.getGroupName())) {
            deRegisterNodeFromGroup(group.getGroupName());
        }
    }

    @Override
    public Set<Group> listLocalGroups() {
        return listGroups(getNode());
    }

    @Override
    public boolean isLocalGroup(String groupName) {
        Set<Group> localGroups = this.listLocalGroups();
        for (Group localGroup : localGroups) {
            if (localGroup.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Group> listAllGroups() {
        return new HashSet<Group>(listGroups().values());
    }

    @Override
    public Group findGroupByName(String groupName) {
        Map<String, Group> groupMap = listGroups();
        return groupMap.get(groupName);
    }
    
    @Override
    public GroupConfiguration findGroupConfigurationByName(String groupName) {
        for (GroupConfiguration groupConfiguration : groupMemberships) {
            if(groupConfiguration.getGroupName().equals(groupName))
                return groupConfiguration;
        }
        return null;
    }

    @Override
    public Map<String, Group> listGroups() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
    }

    @Override
    public Set<Group> listGroups(Node node) {
        Set<Group> result = new HashSet<Group>();

        Map<String, Group> groupMap = listGroups();
        Collection<Group> groupCollection = groupMap.values();
        if (groupCollection != null && !groupCollection.isEmpty()) {
            for (Group group : groupCollection) {
                if (group.getNodes().contains(node)) {
                    result.add(group);
                }
            }
        }
        return result;
    }

    @Override
    public Set<String> listGroupNames() {
        return this.nodeConfiguration.getGroupNames();
    }

    @Override
    public Set<String> listGroupNames(Node node) {
        Set<String> names = new HashSet<String>();
        Map<String, Group> groups = listGroups();

        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups.values()) {
                if (group.getNodes().contains(node)) {
                    names.add(group.getName());
                }
            }
        }
        return names;
    }

    /**
     * Register this node to a new {@link Group} by receiving a {@link GroupConfiguration} object.
     *
     * @param groupName the group configuration for the new group membership for this node.
     */
    @Override
    public void registerGroup(String groupName) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("CELLAR HAZELCAST: registering cluster group {}.", groupName);
        }
        Map<String, Group> groups = listGroups();
        Group group = groups.get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        group.getNodes().add(this.getNode());

        Hashtable serviceProperties = new Hashtable();
        serviceProperties.put("type", "group");
        serviceProperties.put("name", groupName);

        if (!producerRegistrations.containsKey(groupName)) {
            EventProducer producer = groupProducers.get(groupName);
            if (producer == null) {
                producer = eventTransportFactory.getEventProducer(groupName, Boolean.TRUE);
                groupProducers.put(groupName, producer);
            }

            ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, serviceProperties);
            producerRegistrations.put(groupName, producerRegistration);
        }

        if (!consumerRegistrations.containsKey(groupName)) {
            EventConsumer consumer = groupConsumer.get(groupName);
            if (consumer == null) {
                consumer = eventTransportFactory.getEventConsumer(groupName, true);
                groupConsumer.put(groupName, consumer);
            } else if (!consumer.isConsuming()) {
                consumer.start();
            }
            ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, serviceProperties);
            consumerRegistrations.put(groupName, consumerRegistration);
        }

        // launch the synchronization on the group
        try {
            ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(Synchronizer.class.getCanonicalName(), null);
            if (serviceReferences != null && serviceReferences.length > 0) {
                for (ServiceReference ref : serviceReferences) {
                    Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                    if (synchronizer != null && synchronizer.isSyncEnabled(group)) {
                        synchronizer.pull(group);
                        synchronizer.push(group);
                    }
                    bundleContext.ungetService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to look for synchronizers", e);
        }
    }

    @Override
    public void deregisterFromAllGroups() {
        Map<String, Group> groups = listGroups();
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            Group group = entry.getValue();
            // remove local node from cluster group
            group.getNodes().remove(getNode());
            if (group.getNodes().isEmpty()) {
                groups.remove(groupName);
            }
            deRegisterNodeFromGroup(groupName);
        }
    }

    @Override
    public void deRegisterNodeFromGroup(String groupName) {
        // un-register cluster group consumers
        if (consumerRegistrations != null && !consumerRegistrations.isEmpty()) {
            ServiceRegistration consumerRegistration = consumerRegistrations.get(groupName);
            if (consumerRegistration != null) {
                consumerRegistration.unregister();
                consumerRegistrations.remove(groupName);
            }
        }

        // un-register cluster group producers
        if (producerRegistrations != null && !producerRegistrations.isEmpty()) {
            ServiceRegistration producerRegistration = producerRegistrations.get(groupName);
            if (producerRegistration != null) {
                producerRegistration.unregister();
                producerRegistrations.remove(groupName);
            }
        }

        // remove consumers & producers
        groupProducers.remove(groupName);
        EventConsumer consumer = groupConsumer.remove(groupName);
        if (consumer != null) {
            consumer.stop();
        }

        // remove node from group
        try {
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
            Dictionary<String, Object> properties = configuration.getProperties();
            Set<String> groupNames = (Set<String>) properties.get(Configurations.GROUPS_KEY);
            groupNames.remove(groupName);
            properties.put(Configurations.GROUPS_KEY, groupNames);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to read cluster group configuration", e);
        }
    }

    /**
     * Invoked when an entry is added.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryAdded(EntryEvent entryEvent) {
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is removed.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryRemoved(EntryEvent entryEvent) {
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("CELLAR HAZELCAST: cluster group configuration has been updated: " + entryEvent);
        }
    }

    /**
     * Invoked when an entry is evicted.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryEvicted(EntryEvent entryEvent) {
        entryUpdated(entryEvent);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    /**
     * @return the masterCluster
     */
    public CellarCluster getMasterCluster() {
        return masterCluster;
    }

    /**
     * @param masterCluster the masterCluster to set
     */
    public void setMasterCluster(CellarCluster masterCluster) {
        this.masterCluster = masterCluster;
    }

    /**
     * @return the groupMemberships
     */
    public List<GroupConfiguration> getGroupMemberships() {
        return groupMemberships;
    }

    /**
     * @param groupMemberships the groupMemberships to set
     */
    public void setGroupMemberships(List<GroupConfiguration> groupMemberships) {
        this.groupMemberships = groupMemberships;
    }

    /**
     * @return the configurationAdmin
     */
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    /**
     * @param configurationAdmin the configurationAdmin to set
     */
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
