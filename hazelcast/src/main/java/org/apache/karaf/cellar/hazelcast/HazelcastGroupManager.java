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
    private final Map<String, GroupConfiguration> groupMemberships = new ConcurrentHashMap<String, GroupConfiguration>();
    private final Map<String, String> pidGroupNameMap = new HashMap<String, String>();
    private HazelcastCluster masterCluster;
    private ConfigurationAdmin configAdmin;

    public void init() {
    }

    public void destroy() {
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
        LOGGER.warn("Node membership was removed for node configuration: {}.", nodeConfiguration);
        if (this.nodeConfiguration != null) {
            try {
                removeNodeFromAllGroups(false);
            } catch (IOException ex) {
                throw new ConfigurationException(null, "Error when attempting to deregister node from all groups.", ex);
            }
            this.nodeConfiguration = null;
        } else {
            LOGGER.warn("No node was configured to remove, skipping.");
        }
    }

    /**
     * Listens for group configuration admin notifications of group being bound.
     *
     * @param groupConfig the group configuration.
     * @param properties the group service properties.
     * @throws org.osgi.service.cm.ConfigurationException
     */
    public void groupConfigured(GroupConfiguration groupConfig, Map<String, Object> properties) throws ConfigurationException, IOException {
        LOGGER.warn("Group service was created: " + properties);
        this.groupMemberships.put(groupConfig.getName(), groupConfig);
        this.registerGroup(groupConfig, properties);
        if (nodeConfiguration != null) {
            if (nodeConfiguration.getGroups().contains(groupConfig.getName())) {
                this.addNodeToGroupStore(groupConfig.getName());
            }
        }
    }

    /**
     * Listens for group configuration admin notifications of group being unbound.
     *
     * @param group the group configuration.
     * @param properties the group service properties.
     */
    public void groupRemoved(GroupConfiguration group, Map<String, Object> properties) throws IOException, ConfigurationException {
        if (group != null) {
            LOGGER.warn("Group service was removed: " + properties);
            this.groupMemberships.remove(group.getName());
            this.deregisterGroup(group, properties);
        }
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
            if (!nodeConfiguration.getGroups().contains(groupName)) {
                try {
                    this.addNodeToGroupStore(groupName);
                } catch (Exception ex) {
                    throw new ConfigurationException(null, "Error when attempting to register the node to group " + groupName, ex);
                }
            }
        }
    }

    @Override
    public void deregisterGroup(GroupConfiguration groupConfig, Map<String, Object> properties) throws IOException, ConfigurationException {
        LOGGER.warn("A group configuration is removed {}", properties);
        if (groupConfig != null) {
            pidGroupNameMap.remove(groupConfig.getName());
            if (nodeConfiguration != null) {
                removeNodeFromGroupStore(groupConfig.getName());
            } else {
                LOGGER.info("Node configuration is null, group configuration would have been already removed from it, skipping.");
            }
        } else {
            LOGGER.info("Group to remove was null so it was already de-registered, skipping.");
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
     * Removes the specified node as a member of the specified group.
     *
     * @param groupName
     * @throws java.io.IOException
     */
    @Override
    public void deregisterNodeFromGroup(String groupName) throws IOException {
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

    private Group addGrouptoStore(Group group) {
        IMap<String, Group> map = getGroupMapStore();
        return map.put(group.getName(), group);
    }

    private void addNodeToGroupStore(String groupName) throws IOException {
        IMap<String, Group> map = getGroupMapStore();
        map.lock(groupName);
        Group group = map.get(groupName);
        group.addNode(getNode());
        map.put(groupName, group);
        map.unlock(groupName);
        if (nodeConfiguration.getGroups().add(groupName)) {
            this.saveNodeConfiguration();
        }
    }

    protected void removeNodeFromGroupStore(String groupName) throws IOException {
        if (this.isDefaultGroup(groupName)) {
            LOGGER.warn("Can't remove a node from the default group.");
            return;
        }
        IMap<String, Group> map = getGroupMapStore();
        map.lock(groupName);
        Group group = map.get(groupName);
        group.removeNode(getNode());
        map.put(groupName, group);
        map.unlock(groupName);
        if (nodeConfiguration.getGroups().remove(groupName)) {
            this.saveNodeConfiguration();
        }

    }

    private void removeNodeFromAllGroups(boolean save) throws IOException {
        IMap<String, Group> map = getGroupMapStore();
        for (String groupName : map.keySet()) {
            if (save && this.isDefaultGroup(groupName)) {
                LOGGER.warn("Can't remove a node from the default group, even if removing node from al groups, when the node config will be saved.");
                continue;
            }
            map.lock(groupName);
            Group group = map.get(groupName);
            group.removeNode(getNode());
            map.put(groupName, group);
            map.unlock(groupName);
            nodeConfiguration.getGroups().remove(groupName);
        }
        if (save) {
            this.saveNodeConfiguration();

        }
    }

    protected void saveNodeConfiguration() throws IOException {
        Configuration configuration = configAdmin.getConfiguration(NodeConfiguration.class
                .getCanonicalName(), null);
        configuration.update(nodeConfiguration.getProperties());
    }

    private IMap<String, Group> getGroupMapStore() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);

    }

    protected void createNewGroupConfiguration(String groupName) throws IOException {
        Configuration configuration = configAdmin.createFactoryConfiguration(GroupConfiguration.class
                .getCanonicalName(), "?");
        Dictionary<String, Object> properties = configuration.getProperties();
        if (properties
                == null) {
            properties = new Hashtable<String, Object>();
        }

        properties.put(GroupConfigurationImpl.GROUP_NAME_PROPERTY, groupName);

        configuration.update(properties);

        pidGroupNameMap.put(groupName, configuration.getPid());
    }

    protected void deleteGroupConfiguration(String groupName) throws IOException, InvalidSyntaxException {
        String pid = pidGroupNameMap.remove(groupName);
        LOGGER.info("Attempting to delete group configuration {} with pid {}.", groupName, pid);
//                Configuration[] configurations = configAdmin.listConfigurations("(&(" + GroupConfigurationImpl.GROUP_NAME_PROPERTY + "=" + groupName + "))");
        Configuration configuration = configAdmin.getConfiguration(pid);
        configuration.delete();
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
        Set<String> result = new HashSet<String>();

        IMap<String, Group> map = getGroupMapStore();
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
