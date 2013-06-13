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
package org.apache.karaf.cellar.config.shell.completers;

import java.util.Collection;
import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.karaf.cellar.core.CellarCluster;

/**
 * Command completer for the configuration from the cluster.
 */
public class ClusterConfigCompleter implements Completer {

    protected ClusterManager clusterManager;
    
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            Collection<CellarCluster> clusters = clusterManager.getClusters();
            if (!clusters.isEmpty()) {
                for (CellarCluster cluster : clusters) {
                    Map<String, Properties> clusterConfigurations = cluster.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + cluster);
                    if (clusterConfigurations != null && !clusterConfigurations.isEmpty()) {
                        for (String pid : clusterConfigurations.keySet()) {
                            if (delegate.getStrings() != null && !delegate.getStrings().contains(pid)) {
                                delegate.getStrings().add(pid);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }
}
