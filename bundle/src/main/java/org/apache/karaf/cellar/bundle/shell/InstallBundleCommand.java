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
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.osgi.framework.BundleEvent;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

@Command(scope = "cluster", name = "bundle-install", description = "Install a bundle in a cluster group")
public class InstallBundleCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "urls", description = "Bundle URLs separated by whitespace", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases = { "--start" }, description = "Start the bundle after installation", required = false, multiValued = false)
    boolean start;

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        CellarSupport support = new CellarSupport();
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
        List<String> whitelist = groupConfig.getOutboundBundleWhitelist();
        List<String> blacklist = groupConfig.getOutboundBundleBlacklist();
        for (String url : urls) {
            // check if the bundle is allowed to send bundle events
            if (support.isAllowed(url, whitelist, blacklist)) {
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
                Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
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
                event.setSourceGroup(group);
                eventProducer.produce(event);
            } else {
                System.err.println("Bundle location " + url + " is blocked outbound in cluster group " + groupName);
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
