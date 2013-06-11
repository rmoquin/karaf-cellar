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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import java.util.Map;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "feature-list", description = "List the features in a cluster group")
public class ListGroupFeatures extends FeatureCommandSupport {

    protected static final String HEADER_FORMAT = " %-11s   %-15s   %s";
    protected static final String OUTPUT_FORMAT = "[%-11s] [%-15s] %s";

    @Argument(index = 0, name = "cluster", description = "The cluster group name", required = true, multiValued = false)
    String clusterName;

    @Option(name = "-i", aliases = { "--installed" }, description = "Display only installed features", required = false, multiValued = false)
    boolean installed;

    @Override
    protected Object doExecute() throws Exception {
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster group " + clusterName + " doesn't exist");
            return null;
        }
            Map<FeatureInfo, Boolean> clusterFeatures = cluster.getMap(Constants.FEATURES + Configurations.SEPARATOR + clusterName);
            if (clusterFeatures != null && !clusterFeatures.isEmpty()) {
                System.out.println(String.format("Features in cluster group " + clusterName));
                System.out.println(String.format(HEADER_FORMAT, "Status", "Version", "Name"));
                for (FeatureInfo info : clusterFeatures.keySet()) {
                    String name = info.getName();
                    String version = info.getVersion();
                    String statusString = "";
                    boolean status = clusterFeatures.get(info);
                    if (status) {
                        statusString = "installed";
                    } else {
                        statusString = "uninstalled";
                    }
                    if (version == null)
                        version = "";
                    if (!installed || (installed && status)) {
                        System.out.println(String.format(OUTPUT_FORMAT, statusString, version, name));
                    }
                }
            } else System.err.println("No features in cluster group " + clusterName);
        return null;
    }

}
