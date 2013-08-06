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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
    private Map<String, CellarCluster> clusterMap = new ConcurrentHashMap<String, CellarCluster>();

    public void init() {
        this.clusterMap.put(masterCluster.getName(), masterCluster);
    }

    public void destroy() {
        this.clusterMap.clear();
        this.masterCluster = null;
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
            for (Iterator<CellarCluster> it = clusterMap.values().iterator(); it.hasNext();) {
                CellarCluster cellarCluster = it.next();
                Set<Node> nodes = cellarCluster.listNodes();
                for (Node node : nodes) {
                    if (ids.contains(node.getId())) {
                        nodeList.add(node);
                    }
                }
            }
        }
        return nodeList;
    }

    /**
     * Get the nodes with given names.
     *
     * @param names a collection of names to look for.
     * @return a Set containing the nodes.
     */
    @Override
    public Set<Node> listNodesByName(Collection<String> names) {
        Set<Node> nodeList = new HashSet<Node>();
        if (names != null && !names.isEmpty()) {
            for (Iterator<CellarCluster> it = clusterMap.values().iterator(); it.hasNext();) {
                CellarCluster cellarCluster = it.next();
                Set<Node> nodes = cellarCluster.listNodes();
                for (Node node : nodes) {
                    if (names.contains(node.getName())) {
                        nodeList.add(node);
                    }
                }
            }
        }
        return nodeList;
    }

    @Override
    public Node findNodeById(String nodeId) {
        for (Iterator<CellarCluster> it = clusterMap.values().iterator(); it.hasNext();) {
            CellarCluster cellarCluster = it.next();
            Node node = cellarCluster.findNodeById(nodeId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Node findNodeByName(String nodeName) {
        for (Iterator<CellarCluster> it = clusterMap.values().iterator(); it.hasNext();) {
            CellarCluster cellarCluster = it.next();
            Node node = cellarCluster.findNodeByName(nodeName);
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
        List<CellarCluster> list = new ArrayList<CellarCluster>();
        list.addAll(clusterMap.values());
        return list;
    }

    /**
     * @return the masterCluster
     */
    @Override
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
