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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster manager interface.
 */
public interface ClusterManager {
    /**
     * Get a Map in Hazelcast.
     *
     * @param mapName the Map in the main administrative cluster.
     * @return the Map with the specified name.
     */
    public Map getMap(String mapName);

    /**
     * Get a List in the main administrative cluster.
     *
     * @param listName the List name.
     * @return the List with the specified name.
     */
    public List getList(String listName);

    /**
     * Get a Set from the main administrative cluster.
     *
     * @param setName the Set name.
     * @return the Set with the specifed name.
     */
    public Set getSet(String setName);

    /**
     * Get the nodes in the cluster.
     *
     * @return the set of nodes in the cluster.
     */
    public Set<Node> listNodes();

    /**
     * Get the nodes with a given ID.
     *
     * @param ids the collection of ID to look for.
     * @return the set of nodes.
     */
    public Set<Node> listNodes(Collection<String> ids);

    /**
     * @return the clusters
     */
    public Collection<CellarCluster> getClusters();

    /**
     * Get a node identified by a given ID.
     *
     * @param id the id of the node to look for.
     * @return the node.
     */
    public Node findNodeById(String id);

    /**
     * Get a node identified by a given name.
     *
     * @param name the name of the node to look for.
     * @return the node.
     */
    public Node findNodeByName(String name);

    /**
     * Look for a cluster with a given name.
     *
     * @param clusterName the cluster name to look for.
     * @return the cluster found, or null if no cluster found.
     */
    public CellarCluster findClusterByName(String clusterName);

    /**
     * Generate an unique ID across the cluster.
     *
     * @return a unique ID across the cluster.
     */
    public String generateId();

    /**
     * @return the masterCluster
     */
    CellarCluster getMasterCluster();

    /**
     * Get the nodes with given names.
     *
     * @param names a collection of IDs to look for.
     * @return a Set containing the nodes.
     */
    Set<Node> listNodesByName(Collection<String> names);
}
