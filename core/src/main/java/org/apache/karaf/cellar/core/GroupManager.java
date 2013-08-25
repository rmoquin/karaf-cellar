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
package org.apache.karaf.cellar.core;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;

/**
 * Generic cluster group manager interface.
 */
public interface GroupManager {
    /**
     * Get the local node.
     *
     * @return the local node.
     */
    public Node getNode();

    /**
     * Look for a cluster group with the given name.
     *
     * @param groupName the cluster group name to look for.
     * @return the cluster group found, or null if no cluster group found.
     */
    public Group findGroupByName(String groupName);

    /**
     * Look for a cluster group's configuration with the given name.
     *
     * @param groupName the cluster group name to look for.
     * @return the cluster group found, or null if no cluster group found.
     */
    public GroupConfiguration findGroupConfigurationByName(String groupName);

    /**
     * Get the list of cluster groups.
     *
     * @return a map of cluster groups name and cluster groups.
     */
    public Map<String, Group> listGroups();

    /**
     * Get the list of local cluster groups.
     * A "local" cluster group means a cluster group where the local node is belonging.
     *
     * @return a set of local cluster groups.
     */
    public Set<Group> listLocalGroups();

    /**
     * Check if a given cluster group is a local one.
     * A "local" clsuter group means a cluster group where the local node is belonging.
     *
     * @param groupName the cluster group name.
     * @return true if the cluster group is a local one, false else.
     */
    public boolean isLocalGroup(String groupName);

    /**
     * Get the list of all known cellar groups in the cluster.
     *
     * @return the list cluster groups.
     */
    public Set<Group> listAllGroups();

    /**
     * Get the cellar groups a specific node is belongs to.
     *
     * @param node the node.
     * @return the set of cellar groups.
     */
    public Set<Group> listGroups(Node node);

    /**
     * Get the the names of the cellar groups the local node belongs to.
     *
     * @return the set of cellar group names.
     */
    public Set<String> getJoinedGroupNames();

    /**
     * Get the cluster group names "hosting" a given node.
     *
     * @param node the node.
     * @return the set of cluster group names "hosting" the given node.
     */
    public Set<String> listGroupNames(Node node);

    public void registerGroup(GroupConfiguration groupConfig, Map<String, Object> properties) throws ConfigurationException;

    public void deregisterGroup(GroupConfiguration groupConfig, Map<String, Object> properties);

    /**
     * Removes the specified cellar group from the local nodes configuration which triggers the appropriate
     * deregistration actions to be done.
     *
     * @param groupName the group to remove this node from.
     */
    public void deregisterNodeFromGroup(String groupName) throws IOException;

    public void deregisterNodeFromAllGroups() throws IOException;

    void createGroup(String groupName) throws IOException, ConfigurationException;

    void joinGroup(String groupName) throws ConfigurationException;

    void deleteGroup(String groupName) throws IOException, InvalidSyntaxException;

    NodeConfiguration getNodeConfiguration();
}
