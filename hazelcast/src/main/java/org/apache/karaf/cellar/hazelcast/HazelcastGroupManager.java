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
import org.apache.karaf.cellar.hazelcast.internal.GroupConfigurationImpl;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
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
    private CellarCluster masterCluster;
    private ConfigurationAdmin configurationAdmin;

    public void init() {
        IMap groupConfiguration = (IMap) masterCluster.getMap(Configurations.GROUP_CONFIGURATION_DO_STORE);
        groupConfiguration.addEntryListener(this, true);
    }

    public void destroy() {
        Map<String, Group> groupMap = listGroups();
        for (Map.Entry<String, Group> entry : groupMap.entrySet()) {
            Group group = entry.getValue();
            if (group.getNodes().remove(this.getNode())) {
                this.updateGroupInStore(group);
            }
        }
    }

    /**
     * Listens for node configurations to be bound.
     *
     * @param node the node configuration.
     * @param properties the node service properties.
     */
    public void nodeMembershipsReceived(NodeConfiguration nodeConfiguration) throws ConfigurationException {
        this.nodeConfiguration = nodeConfiguration;
        try {
            Set<String> groupList = this.nodeConfiguration.getGroups();
            for (Iterator<String> it = groupList.iterator(); it.hasNext();) {
                String groupName = it.next();
                if (this.pidGroupNameMap.containsKey(groupName)) {
                    registerNodeToGroup(groupName);
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
     * @param node the node configuration.
     * @param properties the node service properties.
     */
    public void nodeMembershipsRemoved(NodeConfiguration nodeConfiguration) {
        if (this.nodeConfiguration != null) {
            this.nodeConfiguration = null;
            this.deregisterNode();
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
        Group group = groupConfig.register();
        this.addGrouptoStore(group);
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        String groupName = groupConfig.getName();
        pidGroupNameMap.put(groupName, servicePid);
        LOGGER.warn("A new group is being registered: {}", groupName);
        if (nodeConfiguration != null) {
            if (!nodeConfiguration.getGroups().contains(groupName)) {
                try {
                    registerNodeToGroup(groupName);
                } catch (Exception ex) {
                    throw new ConfigurationException(null, "Error when attempting to register the node to group " + groupName, ex);
                }
            }
        }
    }

    /**
     * Listens for group configurations to be unbound.
     *
     * @param group the group configuration.
     * @param properties the group service properties.
     */
    public void groupRemoved(GroupConfiguration group, Map<String, Object> properties) {
        LOGGER.warn("A group configuration is removed {}", properties);
        String servicePid = (String) properties.get(org.osgi.framework.Constants.SERVICE_PID);
        if (pidGroupNameMap.containsKey(servicePid)) {
            pidGroupNameMap.remove(servicePid);
        } else {
            LOGGER.info("Group to remove has null pid, skipping.");
        }
        if (group != null) {
            deregisterNodeFromGroup(group.getName());
        } else {
            LOGGER.info("Group to remove was so can't deregister it, skipping.");
        }
    }

    /**
     * Creates a group configuration which will trigger an update that will actually create the group.
     *
     * @param groupName the name of the group.
     * @throws IOException
     * @throws ConfigurationException
     */
    @Override
    public void createGroup(String groupName) throws IOException, ConfigurationException {
        if (pidGroupNameMap.containsKey(groupName)) {
            LOGGER.warn("Group can't be created because it already exists: + " + groupName);
            throw new ConfigurationException(GroupConfigurationImpl.GROUP_NAME_PROPERTY, "The group cannot be created because one already exists with name: " + groupName);
        }

        Configuration configuration = configurationAdmin.createFactoryConfiguration(GroupConfiguration.class.getCanonicalName(), "?");
        Dictionary<String, Object> properties = configuration.getProperties();
        if (properties == null) {
            properties = new Hashtable<String, Object>();
            properties.put(GroupConfigurationImpl.GROUP_NAME_PROPERTY, groupName);
            configuration.update(properties);
        }
    }

    /**
     * Joins a new group by configuring the node to the group which triggers the actual joining of the node to occur.
     *
     * @param groupName
     */
    @Override
    public void joinGroup(String groupName) {
        try {
            if (!nodeConfiguration.getGroups().contains(groupName)) {
                this.registerNodeToGroup(groupName);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to join local node to group, " + groupName, e);
        }
    }

    protected void registerNodeToGroup(String groupName) throws IOException {
        nodeConfiguration.getGroups().add(groupName);
        Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
        configuration.update(nodeConfiguration.getProperties());
        Group group = listGroups().get(groupName);
        if (!group.getNodes().contains(this.getNode())) {
            group.getNodes().add(this.getNode());
            this.updateGroupInStore(group);
        }
    }

    protected void removeNodeFromAllGroups() {
        Map<String, Group> groupMap = listGroups();
        for (Map.Entry<String, Group> entry : groupMap.entrySet()) {
            Group group = entry.getValue();
            if (group.getNodes().remove(this.getNode())) {
                this.updateGroupInStore(group);
            }
        }
    }

    /**
     * Removes a group from the Configuration Admin, which will trigger this node to be removed from the node.
     *
     * @param groupName the group name
     * @throws IOException if an IO exception occurs.
     * @throws InvalidSyntaxException if the group cannot be found using specified filter.
     */
    protected void removeGroup(String groupName) throws IOException, InvalidSyntaxException {
        String pid = pidGroupNameMap.get(groupName);
        if (pid == null) {
            LOGGER.warn("Group can't be deleted because it doesn't exist: + " + groupName);
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Attempting to delete group configuration: " + groupName);
        }
        Configuration[] configurations = configurationAdmin.listConfigurations("service.pid = " + pid);
        //Shouldn't ever be more than one but just in case.
        for (Configuration configuration : configurations) {
            configuration.delete();
        }
        pidGroupNameMap.remove(pid);
    }

    /**
     * Removes all the groups from the this nodes configuration which triggers the appropriate deregistration actions to
     * be done.
     *
     * @param groupName
     */
    @Override
    public void deregisterNodeFromGroup(String groupName) {
        if (Configurations.DEFAULT_GROUP_NAME.equals(groupName)) {
            LOGGER.warn("Cannot deregister node from the default group.");
            return;
        }
        try {
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
            nodeConfiguration.getGroups().remove(groupName);
            configuration.update(nodeConfiguration.getProperties());
            Group group = listGroups().get(groupName);
            if (group.getNodes().remove(this.getNode())) {
                this.updateGroupInStore(group);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to remove node from group being removed " + groupName, e);
        }
    }

    /**
     * Removes all the groups from this nodes configuration which triggers the appropriate deregistration actions to be
     * done.
     */
    @Override
    public void deregisterNode() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
            nodeConfiguration.getGroups().clear();
            nodeConfiguration.getGroups().add(Configurations.DEFAULT_GROUP_NAME);
            configuration.update(nodeConfiguration.getProperties());

            this.removeNodeFromAllGroups();
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to remove local node from all it's groups", e);
        }
    }

    @Override
    public String getPidForGroup(String groupName) {
        return this.pidGroupNameMap.get(groupName);
    }

    @Override
    public Node getNode() {
        return masterCluster.getLocalNode();
    }

    @Override
    public Set<Group> listLocalGroups() {
        return listGroups(getNode());
    }

    protected void addGrouptoStore(Group group) {
        masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE).put(group.getName(), group);
    }

    protected void updateGroupInStore(Group group) {
        masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE).put(group.getName(), group);
    }

    protected void removeGroupFromStore(Group group) {
        masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE).remove(group.getName());
    }

    protected Map<String, Group> getGroupsFromStore() {
        return masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
    }

    @Override
    public boolean isLocalGroup(String groupName) {
        return nodeConfiguration.getGroups().contains(groupName);
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
            if (groupConfiguration.getName().equals(groupName)) {
                return groupConfiguration;
            }
        }
        return null;
    }

    @Override
    public Map<String, Group> listGroups() {
        return getGroupsFromStore();
    }

    @Override
    public Set<Group> listGroups(Node node) {
        Set<Group> result = new HashSet<Group>();

        Map<String, Group> groupMap = listGroups();
        Collection<Group> groupCollection = groupMap.values();
        for (Group group : groupCollection) {
            if (group.getNodes().contains(node)) {
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
        Set<String> names = new HashSet<String>();
        Map<String, Group> groups = listGroups();

        for (Group group : groups.values()) {
            if (group.getNodes().contains(node)) {
                names.add(group.getName());
            }
        }
        return names;
    }

    /**
     * Invoked when an entry is added.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryAdded(EntryEvent entryEvent) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("**********************************************CELLAR HAZELCAST: An entry was added to the cellar distributed object store: " + entryEvent);
        }
    }

    /**
     * Invoked when an entry is removed.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryRemoved(EntryEvent entryEvent) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("*************************************CELLAR HAZELCAST: An entry was removed from cellar distributed object store: " + entryEvent);
        }
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryUpdated(EntryEvent entryEvent) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("*****************************************CELLAR HAZELCAST: cellar distributed object has been updated store: " + entryEvent);
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

    /**
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }
}
