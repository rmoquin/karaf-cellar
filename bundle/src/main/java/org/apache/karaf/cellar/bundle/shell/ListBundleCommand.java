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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.osgi.framework.BundleEvent;

import java.util.Map;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "bundle-list", description = "List the bundles in a cluster")
public class ListBundleCommand extends CellarCommandSupport {

    protected static final String HEADER_FORMAT = " %-4s   %-11s  %s";
    protected static final String OUTPUT_FORMAT = "[%-4s] [%-11s] %s";

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Option(name = "-s", aliases = {}, description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolicName;

    @Option(name = "-l", aliases = {}, description = "Shows the location", required = false, multiValued = false)
    boolean showLocation;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

            Map<String, BundleState> clusterBundles = cluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + clusterName);
            if (clusterBundles != null && !clusterBundles.isEmpty()) {
                System.out.println(String.format("Bundles in cluster group " + clusterName));
                System.out.println(String.format(HEADER_FORMAT, "ID", "State", "Name"));
                int id = 0;
                for (String bundle : clusterBundles.keySet()) {
                    String[] tokens = bundle.split("/");
                    String symbolicName = null;
                    String version = null;
                    if (tokens.length == 2) {
                        symbolicName = tokens[0];
                        version = tokens[1];
                    } else {
                        symbolicName = bundle;
                        version = "";
                    }
                    BundleState state = clusterBundles.get(bundle);

                    String status;
                    switch (state.getStatus()) {
                        case BundleEvent.INSTALLED:
                            status = "Installed";
                            break;
                        case BundleEvent.RESOLVED:
                            status = "Resolved";
                            break;
                        case BundleEvent.STARTED:
                            status = "Active";
                            break;
                        case BundleEvent.STARTING:
                            status = "Starting";
                            break;
                        case BundleEvent.STOPPED:
                            status = "Resolved";
                            break;
                        case BundleEvent.STOPPING:
                            status = "Stopping";
                            break;
                        case BundleEvent.UNINSTALLED:
                            status = "Uninstalled";
                            break;
                        default:
                            status = "";
                            break;
                    }
                    if (showLocation) {
                        System.out.println(String.format(OUTPUT_FORMAT, id, status, state.getLocation()));
                    } else {
                        if (showSymbolicName) {
                            System.out.println(String.format(OUTPUT_FORMAT, id, status, symbolicName + " (" + version + ")"));
                        } else {
                            System.out.println(String.format(OUTPUT_FORMAT, id, status, state.getName() + " (" + version + ")"));
                        }
                    }
                    id++;
                }
            } else {
                System.err.println("No bundle found in cluster group " + clusterName);
            }
        return null;
    }

}
