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
package org.apache.karaf.cellar.core.shell.completer;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.ClusterManager;

/**
 * Abstract cluster group completer.
 */
public abstract class ClusterCompleterSupport implements Completer {

    private ClusterManager clusterManager;

    /**
     * Check if a cluster should be accepted for completion.
     *
     * @param cluster the cluster to check.
     * @return true if the cluster has been accepted, false else.
     */
    protected abstract boolean acceptsCluster(CellarCluster cluster);

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (CellarCluster cluster : clusterManager.getClusters()) {
                if (acceptsCluster(cluster)) {
                    String name = cluster.getName();
                    if (delegate.getStrings() != null && !delegate.getStrings().contains(name)) {
                        delegate.getStrings().add(name);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    /**
     * @return the clusterManager
     */
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    /**
     * @param clusterManager the clusterManager to set
     */
    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }
}
