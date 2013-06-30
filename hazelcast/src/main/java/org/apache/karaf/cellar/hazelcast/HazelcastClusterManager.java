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

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.apache.karaf.cellar.core.ClusterManager;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster manager implementation powered by Hazelcast.
 */
public class HazelcastClusterManager implements ClusterManager {
    private static transient Logger LOGGER = LoggerFactory.getLogger(HazelcastClusterManager.class);
    private CellarCluster masterCluster;
    private List<CellarCluster> clusters;
    private Map<String, CellarCluster> clusterMap = new ConcurrentHashMap<String, CellarCluster>();

    public void init() {
    }

    public void destroy() {
    }

    public void bind(CellarCluster cellarCluster) {
        clusterMap.put(cellarCluster.getName(), masterCluster);
    }

    public void unbind(CellarCluster cellarCluster) {
        clusterMap.remove(cellarCluster.getName());
    }

    /**
     * Get a Map from the main administrative cluster.
     *
     * @param mapName the Map name.
     * @return the Map with the specified name.
     */
    @Override
    public Map getMap(String mapName) {
        return this.masterCluster.getMap(mapName);
    }

    /**
     * Get a List from the main administrative cluster.
     *
     * @param listName the List name.
     * @return the List with the specifed name.
     */
    @Override
    public List getList(String listName) {
        return this.masterCluster.getList(listName);
    }

    /**
     * Get a Set from the main administrative cluster.
     *
     * @param setName the Set name.
     * @return the Set with the specifed name.
     */
    @Override
    public Set getSet(String setName) {
        return this.masterCluster.getSet(setName);
    }

    /**
     * Get a Topic from the main administrative cluster.
     *
     * @param topicName the Topic name.
     * @return the Topic with the specifed name.
     */
    public ITopic getTopic(String topicName) {
        return ((HazelcastClusterManager) this.masterCluster).getTopic(topicName);
    }

    /**
     * Get a Queue from the main administrative cluster.
     *
     * @param queueName the Queue name.
     * @return the Queue with the specifed name.
     */
    public IQueue getQueue(String queueName) {
        return ((HazelcastClusterManager) this.masterCluster).getQueue(queueName);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        return this.masterCluster.listNodes();
    }

    /**
     * Get the nodes with given IDs.
     *
     * @param ids a collection of IDs to look for.
     * @return a Set containing the nodes.
     */
    @Override
    public Set<Node> listNodes(Collection<String> ids) {
        Set<Node> nodeList = new HashSet<Node>();
        if (ids != null && !ids.isEmpty()) {
            Set<Node> nodes = this.masterCluster.listNodes();
            for (Node node : nodes) {
                if (ids.contains(node.getId())) {
                    nodeList.add(node);
                }
            }
        }
        return nodeList;
    }

    @Override
    public Node findNodeById(String nodeId) {
        for (CellarCluster cellarCluster : clusters) {
            Node node = cellarCluster.findNodeById(nodeId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    @Override
    public String generateId() {
        return masterCluster.generateId();
    }

    @Override
    public CellarCluster findClusterByName(String clusterName) {
        return clusterMap.get(clusterName);
    }

    /**
     * @return the clusters
     */
    @Override
    public List<CellarCluster> getClusters() {
        return clusters;
    }

    /**
     * @param clusters the clusters to set
     */
    public void setClusters(List<CellarCluster> clusters) {
        this.clusters = clusters;
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
