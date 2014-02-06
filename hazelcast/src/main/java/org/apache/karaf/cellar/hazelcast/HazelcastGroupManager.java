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
 *//*
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
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

/**
 * A group manager implementation powered by Hazelcast. The role of this class is to provide means of creating groups,
 * setting nodes to groups etc. Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);
    private NodeConfiguration nodeConfiguration;
    private final Map<String, GroupConfiguration> groupMemberships = new ConcurrentHashMap<String, GroupConfiguration>();
    private HazelcastCluster masterCluster;
    private ConfigurationAdmin configAdmin;
    private final Map<String, String> groupConfigListenerIds = new HashMap<String, String>();

    public void init() {
    }

    public void destroy() {
    }

    /**
     * Listens for node configurations to be bound.
     *
     * @param node the node configuration.
     * @throws java.io.IOException
     */
    public void nodeMembershipsReceived(NodeConfiguration node) throws IOException {
        LOGGER.info("Local Node was configured {}.", getNode().getName());
        this.nodeConfiguration = node;
        Set<String> groupList = this.nodeConfiguration.getGroups();
        for (String groupName : groupList) {
            if (this.groupMemberships.containsKey(groupName)) {
                LOGGER.info("Registering node {} to group {}.", getNode().getName(), groupName);
                addNodeToGroupStore(groupName);
            } else {
                LOGGER.warn("No configuration exists yet for group {}, node will be added when it does.", groupName);
            }
        }
    }

    /**
     * Listens for node configurations to be unbound.
     *
     * @param nodeConfiguration the node configuration.
     * @throws java.io.IOException
     */
    public void nodeMembershipsRemoved(NodeConfiguration nodeConfiguration) throws IOException {
        LOGGER.warn("Node membership was removed for node configuration: {}.", nodeConfiguration);
        if (this.nodeConfiguration != null) {
            try {
                removeNodeFromAllGroups(false);
            } finally {
                this.nodeConfiguration = null;
            }
        } else {
            LOGGER.warn("No node was configured to remove, skipping.");
        }
    }

    /**
     * Listens for group configuration admin notifications of group being bound.
     *
     * @param groupConfig the group configuration.
     * @param properties the group service properties.
     * @throws java.io.IOException
     */
    public void groupConfigured(GroupConfiguration groupConfig, Map<String, Object> properties) throws IOException {
        LOGGER.warn("Group service is being registered: " + properties);
        String groupName = groupConfig.getName();
        this.groupMemberships.put(groupName, groupConfig);
        IMap<String, Object> groupConfigStore = this.getGroupConfigurationStore(groupName);
        if (groupConfigStore.isEmpty()) {
            Dictionary<String, Object> localConfiguration = this.getGroupConfiguration(groupName).getProperties();
            for (Enumeration<String> e = localConfiguration.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                Object value = localConfiguration.get(key);
                groupConfigStore.set(key, value);
            }
        }
        if (!this.groupConfigListenerIds.containsKey(groupName)) {
            String id = groupConfigStore.addEntryListener(new GroupConfigurationListener(configAdmin), true);
            this.groupConfigListenerIds.put(groupName, id);
        }

        Group group = groupConfig.register();
        this.addGrouptoNodeStore(group);
        LOGGER.warn("A new group is being registered: {}", groupName);
        if (nodeConfiguration != null) {
            LOGGER.info("Node is configured {}, checking if it's been registered to each group", getNode().getName());
            if (nodeConfiguration.getGroups().contains(groupName)) {
                this.addNodeToGroupStore(groupName);
            }
        }
    }

    /**
     * Listens for group configuration admin notifications of group being unbound.
     *
     * @param group the group configuration.
     * @param properties the group service properties.
     */
    public void groupRemoved(GroupConfiguration group, Map<String, Object> properties) {
        if (group != null) {
            String groupName = group.getName();
            String id = this.groupConfigListenerIds.get(groupName);
            if (id != null) {
                this.getGroupConfigurationStore(groupName).removeEntryListener(id);
            }
            LOGGER.warn("Group service was removed: {}", groupName);
        } else {
            LOGGER.info("Group to remove was null so it was already de-registered, skipping.");
        }
    }

    @Override
    public void createGroup(String groupName) throws IOException {
        this.createOrUpdateGroupConfiguration(groupName);
    }

    @Override
    public void deleteGroup(String groupName) throws ServiceException {
        if (this.isDefaultGroup(groupName)) {
            throw new IllegalArgumentException("Can't delete the default group.");
        }
        this.groupMemberships.remove(groupName);
        removeGroupFromStore(groupName);
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
            this.createOrUpdateGroupConfiguration(groupName);
            this.addNodeToGroupStore(groupName);
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to join local node to group, " + groupName, e);
        }
    }

    /**
     * Removes the specified node as a member of the specified group.
     *
     * @param groupName
     * @throws java.io.IOException
     */
    @Override
    public void deregisterNodeFromGroup(String groupName) throws IOException {
        if (this.isDefaultGroup(groupName)) {
            throw new IllegalArgumentException("Can't remove a node from the default group.");
        }
        this.removeNodeFromGroupStore(groupName);
    }

    /**
     * Removes all the groups from this nodes configuration which triggers the appropriate de-registration actions to be
     * done.
     *
     * @throws java.io.IOException
     */
    @Override
    public void deregisterNodeFromAllGroups() throws IOException {
        this.removeNodeFromAllGroups(true);
    }

    @Override
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    @Override
    public Set<Group> listLocalGroups() {
        return listGroups(getNode());
    }

    private Group addGrouptoNodeStore(Group group) {
        IMap<String, Group> map = getGroupMembershipStore();
        return map.putIfAbsent(group.getName(), group);
    }

    private void removeGroupFromStore(String groupName) {
        IMap<String, Group> map = getGroupMembershipStore();
        map.delete(groupName);
    }

    private void addNodeToGroupStore(String groupName) throws IOException {
        boolean nodeAdded = false;
        IMap<String, Group> map = getGroupMembershipStore();
        map.lock(groupName);
        Group group = map.get(groupName);
        if (!group.containsNode(getNode())) {
            group.addNode(getNode());
            map.set(groupName, group);
            nodeAdded = true;
        }
        map.unlock(groupName);
        if (nodeConfiguration.getGroups().add(groupName)) {
            this.saveNodeConfiguration();
        }
        if (nodeAdded) {
            LOGGER.info("Node was added to distributed group store, invoking synchronization.");
            this.masterCluster.synchronizeNodes(group);
        }
    }

    protected void removeNodeFromGroupStore(String groupName) throws IOException {
        IMap<String, Group> map = getGroupMembershipStore();
        map.lock(groupName);
        Group group = map.get(groupName);
        if (group.containsNode(getNode())) {
            group.removeNode(getNode());
            map.set(groupName, group);
        }
        map.unlock(groupName);
        if (nodeConfiguration.getGroups().remove(groupName)) {
            this.saveNodeConfiguration();
        }

    }

    protected void removeNodeFromAllGroups(boolean save) throws IOException {
        IMap<String, Group> map = getGroupMembershipStore();
        for (String groupName : map.keySet()) {
            if (save && this.isDefaultGroup(groupName)) {
                LOGGER.warn("Can't remove a node from the default group, even if removing node from al groups, when the node config will be saved.");
                continue;
            }
            map.lock(groupName);
            Group group = map.get(groupName);
            group.removeNode(getNode());
            map.set(groupName, group);
            map.unlock(groupName);
            nodeConfiguration.getGroups().remove(groupName);
        }
        if (save) {
            this.saveNodeConfiguration();

        }
    }

    protected void saveNodeConfiguration() throws IOException {
        Configuration configuration = configAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
        configuration.update(nodeConfiguration.getProperties());
    }

    private IMap<String, Group> getGroupMembershipStore() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
    }

    private IMap<String, Object> getGroupConfigurationStore(String groupName) {
        return masterCluster.getMap(groupName);
    }

    private void setGroupConfigurationStore(String groupName, Map<String, Object> properties) {
        masterCluster.getMap(groupName).putAll(properties);
    }

    protected void copyGroupConfigurationLocal(String groupName) throws ServiceException {
        this.createOrUpdateGroupConfiguration(groupName);
    }

    protected void createOrUpdateGroupConfiguration(String groupName) throws ServiceException {
        Hashtable<String, Object> configProperties = new Hashtable<String, Object>();
        Map<String, Object> configurationStore = getGroupConfigurationStore(groupName);
        if (!configurationStore.isEmpty()) {
            configProperties.putAll(configurationStore);
        }
        configProperties.put(GroupConfigurationImpl.GROUP_NAME_PROPERTY, groupName);
        try {
            Configuration configuration = getGroupConfiguration(groupName);
            if (configuration == null) {
                configuration = configAdmin.createFactoryConfiguration(GroupConfiguration.class.getCanonicalName(), "?");
            }
            configuration.update(configProperties);
        } catch (Exception ex) {
            throw new ServiceException("Error occurred while attempting to create/update the local configuration for group + " + groupName, ex);
        }
    }

    protected void deleteGroupConfiguration(String groupName) throws ServiceException {
        LOGGER.info("Delete group configuration {}.", groupName);
        Configuration configuration = getExistingGroupConfiguration(groupName);
        try {
            configuration.delete();
        } catch (Exception ex) {
            throw new ServiceException("Error occurred while attempting to delete the local configuration for group + " + groupName, ex);
        }
        String id = this.groupConfigListenerIds.get(groupName);
        if (id != null) {
            this.getGroupConfigurationStore(groupName).removeEntryListener(id);
        }
    }

    protected Configuration getGroupConfiguration(String groupName) throws ServiceException {
        try {
            Configuration[] configurations = configAdmin.listConfigurations("(" + GroupConfigurationImpl.GROUP_NAME_PROPERTY + "=" + groupName + ")");
            if ((configurations == null) || configurations.length == 0) {
                return null;
            }
            return configurations[0];
        } catch (Exception ex) {
            throw new ServiceException("Error occurred while attempting to retrieve the local configuration for group + " + groupName, ex);
        }
    }

    private Configuration getExistingGroupConfiguration(String groupName) throws ServiceException {
        Configuration configuration = getGroupConfiguration(groupName);
        if (configuration == null) {
            throw new IllegalStateException("No configuration could be found for group: " + groupName);
        }
        return configuration;
    }

    @Override
    public boolean isLocalGroup(String groupName) {
        return nodeConfiguration.getGroups().contains(groupName);
    }

    @Override
    public Set<Group> listAllGroups() {
        return new HashSet<Group>(getGroupMembershipStore().values());
    }

    @Override
    public Group findGroupByName(String groupName) {
        IMap<String, Group> map = getGroupMembershipStore();
        return map.get(groupName);
    }

    @Override
    public GroupConfiguration findGroupConfigurationByName(String groupName) {
        return groupMemberships.get(groupName);
    }

    @Override
    public Map<String, Group> listGroups() {
        return getGroupMembershipStore();
    }

    @Override
    public Set<Group> listGroups(Node node) {
        Set<Group> result = new HashSet<Group>();

        IMap<String, Group> map = getGroupMembershipStore();
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
        Set<String> result = new HashSet<String>();

        IMap<String, Group> map = getGroupMembershipStore();
        for (Iterator<Group> it = map.values().iterator(); it.hasNext();) {
            Group group = it.next();
            if (group.containsNode(node)) {
                result.add(group.getName());
            }
        }
        return result;
    }

    private boolean isDefaultGroup(String groupName) {
        return (Configurations.DEFAULT_GROUP_NAME.equals(groupName));
    }

    public HazelcastCluster getMasterCluster() {
        return masterCluster;
    }

    public void setMasterCluster(HazelcastCluster masterCluster) {
        this.masterCluster = masterCluster;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    @Override
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }
}
