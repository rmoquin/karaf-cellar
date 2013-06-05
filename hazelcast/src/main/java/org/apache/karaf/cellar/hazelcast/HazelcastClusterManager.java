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

import org.apache.karaf.cellar.core.ClusterManager;
import java.util.List;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;

/**
 * Cluster manager implementation powered by Hazelcast.
 */
public class HazelcastClusterManager implements ClusterManager {
    private List<CellarCluster> clusters;

    public void init() {
        
    }

    public void destroy() {
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

    @Override
    public CellarCluster joinCluster(String clusterName) {
        return null;
    }

    @Override
    public void leaveCluster(String clusterName) {
    }

    @Override
    public CellarCluster findClusterByName(String clusterName) {
        return null;
    }

    @Override
    public CellarCluster findNode(Node node) {
        return null;
    }
}
