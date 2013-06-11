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
import org.apache.karaf.cellar.bundle.ClusterBundleEvent;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.osgi.framework.BundleEvent;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "bundle-install", description = "Install bundles in a cluster")
public class InstallBundleCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "urls", description = "Bundle URLs separated by whitespace", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases = {"--start"}, description = "Start the bundle after installation", required = false, multiValued = false)
    boolean start;

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if the exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);

        for (String url : urls) {
            // check if the bundle is allowed
            if (support.isAllowed(cluster.getName(), Constants.CATEGORY, url, EventType.OUTBOUND)) {

                // get the name and version in the location MANIFEST
                JarInputStream jarInputStream = new JarInputStream(new URL(url).openStream());
                Manifest manifest = jarInputStream.getManifest();
                String name = manifest.getMainAttributes().getValue("Bundle-Name");
                String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
                if (name == null) {
                    name = symbolicName;
                }
                if (name == null) {
                    name = url;
                }
                String version = manifest.getMainAttributes().getValue("Bundle-Version");
                jarInputStream.close();

                    // update the cluster
                    Map<String, BundleState> clusterBundles = cluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + clusterName);
                    BundleState state = new BundleState();
                    state.setName(name);
                    state.setLocation(url);
                    if (start) {
                        state.setStatus(BundleEvent.STARTED);
                    } else {
                        state.setStatus(BundleEvent.INSTALLED);
                    }
                    clusterBundles.put(symbolicName + "/" + version, state);

                // broadcast the cluster event
                ClusterBundleEvent event = new ClusterBundleEvent(symbolicName, version, url, BundleEvent.INSTALLED);
                event.setSourceCluster(cluster);
                eventProducer.produce(event);
            } else {
                System.err.println("Bundle location " + url + " is blocked outbound for cluster." + clusterName);
            }
        }

        return null;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
