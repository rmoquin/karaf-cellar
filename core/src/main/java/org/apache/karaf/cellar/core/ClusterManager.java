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
     * @return the clusters
     */
    public Collection<CellarCluster> getClusters();

    public boolean isLocalCluster(CellarCluster cluster);

    public Collection<CellarCluster> getLocalClusters();

    public Map<String, CellarCluster> getClusterMap();

    /**
     * Retrieves all nodes from all clusters.
     *
     * @return all nodes from all known clusters.
     */
    public Set<Node> listNodesAllClusters();

	 /**
     * Get the nodes with a given ID.
     *
     * @param ids the collection of ID to look for.
     * @return the set of nodes.
     */
//    public Set<Node> listNodes(Collection<String> ids);

    /**
     * Get the nodes in a given cluster group.
     *
     * @param group the cluster group.
     * @return the set of nodes in the cluster group.
     */
//    public Set<Node> listNodesByGroup(Group group);

    /**
     * Get a node identified by a given ID.
     *
     * @param id the id of the node to look for.
     * @return the node.
     */
//    public Node findNodeById(String id);

    /**
     * Create a new cluster.
     *
     * @param clusterName the new cluster name.
     * @return the created cluster.
     */
    public void joinCluster(String clusterName);

    /**
     * Delete an existing cluster.
     *
     * @param clusterName the cluster name to delete.
     */
    public void leaveCluster(String clusterName);

    public void deleteCluster(String clusterName);

    /**
     * Look for a cluster with a given name.
     *
     * @param clusterName the cluster name to look for.
     * @return the cluster found, or null if no cluster found.
     */
    public CellarCluster findClusterByName(String clusterName);

    public Node findNodeById(String nodeId);

    public String generateId();

    public CellarCluster getMasterCluster();

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
     * @return the consumer
     */
    public boolean isConsumer();

    /**
     * @return the enableBundleEvents
     */
    public boolean isEnableBundleEvents();

    /**
     * @return the enableClusterEvents
     */
    public boolean isEnableClusterEvents();

    /**
     * @return the enableConfigurationEvents
     */
    public boolean isEnableConfigurationEvents();

    /**
     * @return the enableDOSGIEvents
     */
    public boolean isEnableDOSGIEvents();

    /**
     * @return the enableFeatureEvents
     */
    public boolean isEnableFeatureEvents();

    /**
     * @return the enableOBRBundleEvents
     */
    public boolean isEnableOBRBundleEvents();

    /**
     * @return the enableObrEvents
     */
    public boolean isEnableObrEvents();

    /**
     * @return the producer
     */
    public boolean isProducer();

    public void createCluster(String clusterName);
}
