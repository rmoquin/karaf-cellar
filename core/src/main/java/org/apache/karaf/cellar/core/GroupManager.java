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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Get the list of all cluster groups "hosting" the local node.
     *
     * @return the list of all cluster groups "hosting" the local node.
     */
    public Set<Group> listAllGroups();

    /**
     * Get the cluster groups where a given node is belonging.
     *
     * @param node the node.
     * @return the set of cluster groups "hosting" the node.
     */
    public Set<Group> listGroups(Node node);

    /**
     * Get the cluster group names "hosting" the local node.
     *
     * @return the set of cluster group names "hosting" the local node.
     */
    public List<String> listGroupNames();

    /**
     * Get the cluster group names "hosting" a given node.
     *
     * @param node the node.
     * @return the set of cluster group names "hosting" the given node.
     */
    public List<String> listGroupNames(Node node);

    /**
     * Register the local node in a given cluster group.
     *
     * @param groupName the cluster group name to join.
     */
    public void registerGroup(String groupName);

    /**
     * Un-register the local node from a given cluster group.
     *
     * @param groupName the group to remove this node from.
     */
    void deRegisterNodeFromGroup(String groupName);

    void deregisterFromAllGroups();

    String getPidForGroup(String groupName);

    void createGroup(String groupName) throws IOException, ConfigurationException;

    void joinGroup(String groupName);
}
