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
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.GroupMembershipConfig;
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
    private List<GroupMembershipConfig> currentGroups;
    private Map<String, GroupMembershipConfig> currentGroupsMap = new HashMap<String, GroupMembershipConfig>();
    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private EventTransportFactory eventTransportFactory;
    private CellarCluster masterCluster;

    public void init() {
        IMap groupConfiguration = (IMap) masterCluster.getMap(Configurations.NODE_SYNC_RULES_MAP_DO);
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

    public void updated(Map<String, Object> properties) {
        if (properties != null) {
            if (LOGGER.isInfoEnabled()) {
//                this.switchConfig.updated(properties);
                String groups = (String) properties.get(Configurations.GROUPS_KEY);
                Set<String> groupNames = convertStringToSet(groups);
                if (groupNames != null && !groupNames.isEmpty()) {
                    for (String groupName : groupNames) {
                        registerGroup(groupName);
                    }
                }
            }
        }
    }

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
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    public void joinLocalGroup(GroupMembershipConfig group) {
       LOGGER.warn("A NEW GROUP WAS REGISTERED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + group);
    }
    
    public void leaveLocalGroup(GroupMembershipConfig group) {
        LOGGER.warn("A GROUP WAS DEREGISTERED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + group);
    }
    
    @Override
    public Group createGroup(String groupName) {
        Map<String, Group> groupMap = listGroups();
        Group group = groupMap.get(groupName);
        if (group == null) {
            group = new Group(groupName);
            groupMap.put(groupName, group);
            //syncRules.setProperty(groupName, group);
        }
        group.getNodes().add(this.getNode());

        // add group to configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP_MEMBERSHIP_DETAILS_PID);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties != null) {
                    String groups = (String) properties.get(Configurations.GROUPS_KEY);
                    Set<String> groupNamesSet = convertStringToSet(groups);
                    groupNamesSet.add(groupName);
                    groups = convertSetToString(groupNamesSet);
                    properties.put(Configurations.GROUPS_KEY, groups);
                    configuration.update(properties);
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: error reading cluster group configuration {}", group);
        }
        return group;
    }

    @Override
    public void deleteGroup(String groupName) {
        if (!groupName.equals(Configurations.DEFAULT_GROUP_NAME)) {
            Map<String, Group> groupMap = listGroups();
            groupMap.remove(groupName);
            try {
                // store the group list to configuration admin
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Saving the group membership configuration.");
                }
                Configuration configuration = this.configurationAdmin.getConfiguration(Configurations.GROUP_MEMBERSHIP_DETAILS_PID, "?");
                Dictionary<String, Object> props = configuration.getProperties();
                props.put(Configurations.GROUPS_KEY, groupMap.keySet());
                configuration.update();
            } catch (Exception e) {
                LOGGER.warn("CELLAR HAZELCAST: can't store cluster group list", e);
            }
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
    public Map<String, Group> listGroups() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_DETAILS_MAP_DO);
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
        return listGroupNames(getNode());
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
     * Register this node to a new {@link Group}.
     *
     * @param group the name of the group to add this node to.
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
        createGroup(groupName);

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
            ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
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

        // remove cluster group from configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP_MEMBERSHIP_DETAILS_PID, "?");
            Dictionary<String, Object> properties = configuration.getProperties();
            String groupKeys = (String) properties.get(Configurations.GROUPS_KEY);
            if (groupKeys.contains(groupName)) {
                Set<String> groupNamesSet = convertStringToSet(groupKeys);
                groupNamesSet.remove(groupName);
                groupKeys = convertSetToString(groupNamesSet);
                properties.put(Configurations.GROUPS_KEY, groupKeys);
                configuration.update(properties);

            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to read cluster group configuration", e);
        }
    }

    /**
     * Util method which converts a Set to a String.
     *
     * @param set the Set to convert.
     * @return the String corresponding to the Set.
     */
    protected String convertSetToString(Set<String> set) {
        StringBuilder result = new StringBuilder();
        Iterator<String> groupIterator = set.iterator();
        while (groupIterator.hasNext()) {
            String name = groupIterator.next();
            result.append(name);
            if (groupIterator.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    /**
     * Get a Queue from the main administrative cluster.
     *
     * @param queueName the Queue name.
     * @return the Queue with the specifed name.
     */
    protected Set<String> convertStringToSet(String string) {
        Set<String> result = new HashSet<String>();
        if (string == null) {
            return result;
        }

        String[] groupNames = string.split(",");
        if (groupNames != null && groupNames.length > 0) {
            result.addAll(Arrays.asList(groupNames));
        } else {
            result.add(string);
        }
        return result;
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
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("CELLAR HAZELCAST: cluster group configuration has been updated, updating local configuration");
        }
        try {
            Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUP_MEMBERSHIP_DETAILS_PID);
            Dictionary props = conf.getProperties();
            Object key = entryEvent.getKey();
            Object value = entryEvent.getValue();
            if (props.get(key) == null || !props.get(key).equals(value)) {
                props.put(key, value);
                conf.update(props);
            }
        } catch (Exception ex) {
            LOGGER.warn("CELLAR HAZELCAST: failed to update local configuration", ex);
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

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
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
     * @return the currentGroups
     */
    public List<GroupMembershipConfig> getCurrentGroups() {
        return currentGroups;
    }

    /**
     * @param currentGroups the currentGroups to set
     */
    public void setCurrentGroups(List<GroupMembershipConfig> currentGroups) {
        this.currentGroups = currentGroups;
    }
}
