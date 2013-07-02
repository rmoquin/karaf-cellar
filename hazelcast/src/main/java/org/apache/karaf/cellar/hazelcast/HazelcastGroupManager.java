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
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;

/**
 * A group manager implementation powered by Hazelcast.
 * The role of this class is to provide means of creating groups, setting nodes to groups etc.
 * Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager, EntryListener, ConfigurationListener {
    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);
    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private ConfigurationAdmin configurationAdmin;
    private EventTransportFactory eventTransportFactory;
    private CellarCluster masterCluster;

    public void init() {
        // create a listener for group configuration.
        IMap groupConfiguration = (IMap) masterCluster.getMap(Configurations.GROUPS_CONFIG);
        IMap mapConfiguration = (IMap) masterCluster.getMap(Configurations.GROUPS);
        groupConfiguration.addEntryListener(this, true);
        mapConfiguration.addEntryListener(this, true);
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUPS);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties == null) {
                    properties = new Hashtable<String, Object>();
                }
                String groups = (String) properties.get(Configurations.GROUPS_KEY);
                Set<String> groupNames = convertStringToSet(groups);
                if (groupNames != null && !groupNames.isEmpty()) {
                    for (String groupName : groupNames) {
                        createGroup(groupName);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("CELLAR HAZELCAST: can't create cluster group from configuration admin", e);
        }
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties == null) {
                    properties = new Hashtable<String, Object>();
                }
                String groups = (String) properties.get(Configurations.GROUPS_KEY);
                Set<String> groupNames = convertStringToSet(groups);
                if (groupNames != null && !groupNames.isEmpty()) {
                    for (String groupName : groupNames) {
                        try {
                            registerGroup(groupName);
                        } catch (Exception ex) {
                            LOGGER.error("An error occurred while trying to register group " + groupName, ex);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: can't set group membership for the current node", e);
        }
    }

    public void destroy() {
        Node local = this.getNode();
\        Set<Group> groups = this.listGroups(local);
        for (Group group : groups) {
            String groupName = group.getName();
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
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    @Override
    public Group createGroup(String groupName) {
        Map<String, Group> groupMap = listGroups();
        Group group = groupMap.get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        if (!groupMap.containsKey(groupName)) {
            copyGroupConfiguration(Configurations.DEFAULT_GROUP_NAME, groupName);
            groupMap.put(groupName, group);
            try {
                // store the group list to configuration admin
                persist(groupMap);
            } catch (Exception e) {
                LOGGER.warn("CELLAR HAZELCAST: can't store cluster group list", e);
            }
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
                persist(groupMap);
            } catch (Exception e) {
                LOGGER.warn("CELLAR HAZELCAST: can't store cluster group list", e);
            }
        }
    }

    /**
     * Store the group names in configuration admin.
     *
     * @param groups the list of group to store.
     * @throws Exception in case of storage failure.
     */
    private void persist(Map<String, Group> groups) throws Exception {
        // create group stored in configuration admin
        Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUPS);
        if (configuration != null) {
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                properties = new Hashtable<String, Object>();
            }
            properties.put(Configurations.GROUPS_KEY, convertSetToString(groups.keySet()));
            configuration.update(properties);
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
        return masterCluster.getMap(Configurations.GROUPS);
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
     * Register a cluster {@link Group}.
     *
     * @param group the cluster group to register.
     */
    @Override
    public void registerGroup(Group group) {
        String groupName = group.getName();
        createGroup(groupName);

        LOGGER.info("CELLAR HAZELCAST: registering cluster group {}.", groupName);
        Properties serviceProperties = new Properties();
        serviceProperties.put("type", "group");
        serviceProperties.put("name", groupName);

        if (!producerRegistrations.containsKey(groupName)) {
            EventProducer producer = groupProducers.get(groupName);
            if (producer == null) {
                producer = eventTransportFactory.getEventProducer(groupName, Boolean.TRUE);
                groupProducers.put(groupName, producer);
            }

            ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, (Dictionary) serviceProperties);
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
            ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, (Dictionary) serviceProperties);
            consumerRegistrations.put(groupName, consumerRegistration);
        }

        group.getNodes().add(getNode());
        listGroups().put(groupName, group);

        // add group to configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties != null) {
                    String groups = (String) properties.get(Configurations.GROUPS_KEY);
                    if (groups == null || groups.isEmpty()) {
                        groups = groupName;
                    } else {
                        Set<String> groupNamesSet = convertStringToSet(groups);
                        groupNamesSet.add(groupName);
                        groups = convertSetToString(groupNamesSet);
                    }

                    if (groups == null || groups.isEmpty()) {
                        groups = groupName;
                    }
                    properties.put(Configurations.GROUPS_KEY, groups);
                    configuration.update(properties);
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: error reading cluster group configuration {}", group);
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
    public void registerGroup(String groupName) {
        Map<String, Group> groups = listGroups();
        Group group = groups.get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        registerGroup(group);
    }

    @Override
    public void unRegisterGroup(String groupName) {
        Map<String, Group> groups = listGroups();
        Group group = groups.get(groupName);
        unRegisterGroup(group);
    }

    @Override
    public void unRegisterGroup(Group group) {
        String groupName = group.getName();
        // remove local node from cluster group
        group.getNodes().remove(getNode());
        Map<String, Group> groups = listGroups();
        groups.put(groupName, group);

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
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            Dictionary<String, Object> properties = configuration.getProperties();
            String groupKeys = (String) properties.get(Configurations.GROUPS_KEY);
            if (groupKeys == null || groupKeys.isEmpty()) {
                groupKeys = "";
            } else if (groupKeys.contains(groupName)) {
                Set<String> groupNamesSet = convertStringToSet(groupKeys);
                groupNamesSet.remove(groupName);
                groupKeys = convertSetToString(groupNamesSet);
            }
            properties.put(Configurations.GROUPS_KEY, groupKeys);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to read cluster group configuration", e);
        }
    }

    /**
     * Copy the configuration of a cluster {@link Group}.
     *
     * <b>1.</b> Updates configuration admin from Hazelcast using source config.
     * <b>2.</b> Creates target configuration both on Hazelcast and configuration admin.
     *
     * @param sourceGroupName the source cluster group.
     * @param targetGroupName the target cluster group.
     */
    public void copyGroupConfiguration(String sourceGroupName, String targetGroupName) {
        try {
            Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUPS);
            if (conf != null) {

                // get configuration from config admin
                Dictionary configAdminProperties = conf.getProperties();
                if (configAdminProperties == null) {
                    configAdminProperties = new Properties();
                }
                // get configuration from Hazelcast
                Map<String, String> sourceGroupConfig = masterCluster.getMap(Configurations.GROUPS_CONFIG);

                // update local configuration from cluster
                for (Map.Entry<String, String> parentEntry : sourceGroupConfig.entrySet()) {
                    configAdminProperties.put(parentEntry.getKey(), parentEntry.getValue());
                }

                Dictionary updatedProperties = new Properties();
                Enumeration keyEnumeration = configAdminProperties.keys();
                while (keyEnumeration.hasMoreElements()) {
                    String key = (String) keyEnumeration.nextElement();
                    String value = (String) configAdminProperties.get(key);

                    if (key.startsWith(sourceGroupName)) {
                        String newKey = key.replace(sourceGroupName, targetGroupName);
                        updatedProperties.put(newKey, value);
                        sourceGroupConfig.put(key, value);
                    }
                    updatedProperties.put(key, value);
                }

                conf.update(updatedProperties);
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
        if (string == null) {
            return Collections.EMPTY_SET;
        }
        Set<String> result = new HashSet<String>();
        String[] groupNames = string.split(",");
        if (groupNames != null && groupNames.length > 0) {
            result.addAll(Arrays.asList(groupNames));
        } else {
            result.add(string);
        }
        return result;
    }

    /**
     * Get a Map from the main administrative cluster.
     *
     * @param mapName the Map name.
     * @return the Map with the specifed name.
     */
    @Override
    public void configurationEvent(ConfigurationEvent configurationEvent) {
        String pid = configurationEvent.getPid();
        if (pid.equals(Configurations.GROUPS)) {
            Map groupConfiguration = masterCluster.getMap(Configurations.GROUPS_CONFIG);
            try {
                Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUPS);
                Dictionary properties = conf.getProperties();
                Enumeration keyEnumeration = properties.keys();
                while (keyEnumeration.hasMoreElements()) {
                    Object key = keyEnumeration.nextElement();
                    Object value = properties.get(key);
                    if (!groupConfiguration.containsKey(key) || groupConfiguration.get(key) == null || !groupConfiguration.get(key).equals(value)) {
                        groupConfiguration.put(key, value);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("CELLAR HAZELCAST: failed to update cluster group configuration", e);
            }
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
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        LOGGER.info("CELLAR HAZELCAST: cluster group configuration has been updated, updating local configuration");
        try {
            Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUPS);
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
}
