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

import com.hazelcast.core.IMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.hazelcast.internal.GroupConfigurationImpl;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;

/**
 * A group manager implementation powered by Hazelcast. The role of this class is to provide means of creating groups,
 * setting nodes to groups etc. Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);
    private NodeConfiguration nodeConfiguration;
    private Map<String, GroupConfiguration> groupMemberships = new ConcurrentHashMap<String, GroupConfiguration>();
    private final Map<String, String> pidGroupNameMap = new HashMap<String, String>();
    private HazelcastCluster masterCluster;
    private ConfigurationAdmin configurationAdmin;

    public void init() {
    }

    public void destroy() {
        try {
            this.removeNodeFromAllGroups(false);
        } catch (IOException ex) {
            LOGGER.error("Error trying to deregister node from all groups for shutdown.", ex);
        }
    }

    /**
     * Listens for node configurations to be bound.
     *
     * @param node the node configuration.
     * @throws org.osgi.service.cm.ConfigurationException
     */
    public void nodeMembershipsReceived(NodeConfiguration node) throws ConfigurationException {
        this.nodeConfiguration = node;
        try {
            Set<String> groupList = this.nodeConfiguration.getGroups();
            for (String groupName : groupList) {
                if (this.pidGroupNameMap.containsKey(groupName)) {
                    addNodeToGroupStore(groupName);
                } else {
                    LOGGER.warn("No configuration exists yet for group {}, node will be added when it does.", groupName);
                }
            }
        } catch (Exception ex) {
            throw new ConfigurationException(null, "Error handling configuration node update", ex);
        }
    }

    /**
     * Listens for node configurations to be unbound.
     *
     * @param nodeConfiguration the node configuration.
     * @throws org.osgi.service.cm.ConfigurationException
     */
    public void nodeMembershipsRemoved(NodeConfiguration nodeConfiguration) throws ConfigurationException {
        if (this.nodeConfiguration != null) {
            try {
                removeNodeFromAllGroups(false);
            } catch (IOException ex) {
                throw new ConfigurationException(null, "Error when attempting to deregister node from all groups.", ex);
            }
            this.nodeConfiguration = null;
        } else {
            LOGGER.info("No node was configured to remove, skipping.");
        }
    }

    /**
     * Listens for group configurations to be bound.
     *
     * @param groupConfig the group configuration.
     * @param properties the group service properties.
     */
    public void groupConfigured(GroupConfiguration groupConfig, Map<String, Object> properties) throws ConfigurationException {
        LOGGER.warn("Group service was created: " + properties);
        this.groupMemberships.put(groupConfig.getName(), groupConfig);
        this.registerGroup(groupConfig, properties);
    }

    /**
     * Listens for group configurations to be unbound.
     *
     * @param group the group configuration.
     * @param properties the group service properties.
     */
    public void groupRemoved(GroupConfiguration group, Map<String, Object> properties) {
        LOGGER.warn("Group service was removed: " + properties);
        this.deregisterGroup(group, properties);
    }

    @Override
    public void registerGroup(GroupConfiguration groupConfig, Map<String, Object> properties) throws ConfigurationException {
        Group group = groupConfig.register();
        this.addGrouptoStore(group);
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        String groupName = groupConfig.getName();
        pidGroupNameMap.put(groupName, servicePid);
        LOGGER.warn("A new group is being registered: {}", groupName);
        if (nodeConfiguration != null) {
            if (nodeConfiguration.getGroups().contains(groupName)) {
                try {
                    this.addNodeToGroupStore(groupName);
                } catch (Exception ex) {
                    throw new ConfigurationException(null, "Error when attempting to register the node to group " + groupName, ex);
                }
            }
        }
    }

    @Override
    public void deregisterGroup(GroupConfiguration groupConfig, Map<String, Object> properties) {
        LOGGER.warn("A group configuration is removed {}", properties);
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        if (pidGroupNameMap.containsKey(servicePid)) {
            pidGroupNameMap.remove(servicePid);
        } else {
            LOGGER.info("Group to remove has null pid, skipping.");
        }
        if (groupConfig != null) {
            removeNodeFromGroupStore(groupConfig.getName());
        } else {
            LOGGER.info("Group to remove was so can't deregister it, skipping.");
        }
    }

    @Override
    public void createGroup(String groupName) throws IOException, ConfigurationException {
        if (pidGroupNameMap.containsKey(groupName)) {
            LOGGER.warn("Group can't be created because it already exists: + " + groupName);
            throw new ConfigurationException(GroupConfigurationImpl.GROUP_NAME_PROPERTY, "The group cannot be created because one already exists with name: " + groupName);
        }
        this.createNewGroupConfiguration(groupName);
    }

    @Override
    public void deleteGroup(String groupName) throws IOException, InvalidSyntaxException {
        this.deleteGroupConfiguration(groupName);
    }

    /**
     * Joins a new group by configuring the node to the group which triggers the actual joining of the node to occur.
     *
     * @param groupName
     */
    @Override
    public void joinGroup(String groupName) {
        try {
            this.addNodeToGroupStore(groupName);
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to join local node to group, " + groupName, e);
        }
    }

    /**
     * Removes all the groups from the this nodes configuration which triggers the appropriate de-registration actions
     * to be done.
     *
     * @param groupName
     * @throws java.io.IOException
     */
    @Override
    public void deregisterNodeFromGroup(String groupName) throws IOException {
        this.removeGroupFromNodeConfiguration(groupName);
    }

    /**
     * Removes all the groups from this nodes configuration which triggers the appropriate de-registration actions to be
     * done.
     *
     * @throws java.io.IOException
     */
    @Override
    public void deregisterNodeFromAllGroups() throws IOException {
        this.removeNodeFromAllGroups(false);
    }

    @Override
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    @Override
    public Set<Group> listLocalGroups() {
        return listGroups(getNode());
    }

    protected Group addGrouptoStore(Group group) {
        IMap<String, Group> map = masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
        return map.putIfAbsent(group.getName(), group);
    }

    protected void addNodeToGroupStore(String groupName) throws IOException {
        IMap<String, Group> map = masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
        map.lock(groupName);
        Group group = map.get(groupName);
        group.addNode(getNode());
        map.put(groupName, group);
        map.unlock(groupName);
        this.addGroupToNodeConfiguration(groupName);
    }

    protected void removeGroupStore(String groupName) {
        IMap<String, Group> map = masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
        map.lock(groupName);
        Group group = map.get(groupName);
        group.removeNode(getNode());
        map.put(groupName, group);
        map.unlock(groupName);
    }

    protected void removeNodeFromGroupStore(String groupName) {
        IMap<String, Group> map = masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
        map.lock(groupName);
        Group group = map.get(groupName);
        group.removeNode(getNode());
        map.put(groupName, group);
        map.unlock(groupName);
    }

    protected void removeNodeFromAllGroups(boolean saveNodeConfig) throws IOException {
        IMap<String, Group> map = getGroupMapStore();
        Set<String> groupNames = nodeConfiguration.getGroups();
        for (Iterator<String> it = groupNames.iterator(); it.hasNext();) {
            String groupName = it.next();
            map.lock(groupName);
            Group group = map.get(groupName);
            group.addNode(getNode());
            map.put(groupName, group);
            map.unlock(groupName);
        }
        if (saveNodeConfig) {
            this.removeAllGroupsFromNodeConfiguration();
        }
    }

    @Override
    public boolean isLocalGroup(String groupName) {
        return nodeConfiguration.getGroups().contains(groupName);
    }

    @Override
    public Set<Group> listAllGroups() {
        return new HashSet<Group>(getGroupMapStore().values());
    }

    @Override
    public boolean isProducibleEvent(Object event) {
        return this.nodeConfiguration.isProducer() && this.nodeConfiguration.getEnabledEvents().contains(event);
    }

    @Override
    public Group findGroupByName(String groupName) {
        IMap<String, Group> map = getGroupMapStore();
        return map.get(groupName);
    }

    @Override
    public GroupConfiguration findGroupConfigurationByName(String groupName) {
        return groupMemberships.get(groupName);
    }

    @Override
    public Map<String, Group> listGroups() {
        return getGroupMapStore();
    }

    @Override
    public Set<Group> listGroups(Node node) {
        Set<Group> result = new HashSet<Group>();

        IMap<String, Group> map = getGroupMapStore();
        for (Iterator<Group> it = map.values().iterator(); it.hasNext();) {
            Group group = it.next();
            if (group.containsNode(node)) {
                result.add(group);
            }
        }
        return result;
    }

    @Override
    public Set<String> getJoinedGroupNames() {
        return this.nodeConfiguration.getGroups();
    }

    @Override
    public Set<String> listGroupNames(Node node) {
        return nodeConfiguration.getGroups();
    }

    private IMap<String, Group> getGroupMapStore() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
    }

    private void addGroupToNodeConfiguration(String groupName) throws IOException {
        if (!nodeConfiguration.getGroups().contains(groupName)) {
            nodeConfiguration.getGroups().add(groupName);
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
            configuration.update(nodeConfiguration.getProperties());
        }
    }

    private void removeGroupFromNodeConfiguration(String groupName) throws IOException {
        Set<String> groups = nodeConfiguration.getGroups();
        if (groups.contains(groupName)) {
            groups.remove(groupName);
            if (groups.isEmpty()) {
                LOGGER.warn("Node, {}, was removed from all it's groups, it will be placed into the default group.", this.getNode().getName());
                groups.add(Configurations.DEFAULT_GROUP_NAME);
            }
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class
                    .getCanonicalName(), null);
            configuration.update(nodeConfiguration.getProperties());
        }
    }

    private void removeAllGroupsFromNodeConfiguration() throws IOException {
        Set<String> groups = nodeConfiguration.getGroups();
        if (groups.size() > 0) {
            groups.clear();
            LOGGER.warn("Node, {}, was removed from all it's groups, it will be placed into the default group.", this.getNode().getName());
            groups.add(Configurations.DEFAULT_GROUP_NAME);
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class
                    .getCanonicalName(), null);
            configuration.update(nodeConfiguration.getProperties());
        }
    }

    protected void createNewGroupConfiguration(String groupName) throws IOException {
        Configuration configuration = configurationAdmin.createFactoryConfiguration(GroupConfiguration.class
                .getCanonicalName(), "?");
        Dictionary<String, Object> properties = configuration.getProperties();
        if (properties == null) {
            properties = new Hashtable<String, Object>();
        }
        properties.put(GroupConfigurationImpl.GROUP_NAME_PROPERTY, groupName);
        configuration.update(properties);
    }

    protected void deleteGroupConfiguration(String groupName) throws IOException, InvalidSyntaxException {
        String pid = pidGroupNameMap.get(groupName);
        if (pid == null) {
            LOGGER.warn("Group {} can't be deleted because it doesn't exist.", groupName);
            return;
        }
        LOGGER.info("Attempting to delete group configuration {}.", groupName);
        Configuration[] configurations = configurationAdmin.listConfigurations("(service.pid = " + pid + ")");
        //Shouldn't ever be more than one but just in case.
        if (configurations != null) {
            for (Configuration configuration : configurations) {
                configuration.delete();
            }
        } else {
            LOGGER.warn("There were no configurations to delete for pid filter: (service.pid = {}", pid);
            configurations = configurationAdmin.listConfigurations(null);
            //Shouldn't ever be more than one but just in case.
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    LOGGER.debug("Debugging list of a valid config, service.pid {}, serviceFactory.pid {}, properties {}.", configuration.getPid(), configuration.getFactoryPid(), configuration.getProperties());
                }
            }
        }
        pidGroupNameMap.remove(pid);
    }

    public HazelcastCluster getMasterCluster() {
        return masterCluster;
    }

    public void setMasterCluster(HazelcastCluster masterCluster) {
        this.masterCluster = masterCluster;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }
}
