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
import org.apache.karaf.shell.commands.Command;
import org.osgi.framework.BundleEvent;

import java.util.Map;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "bundle-start", description = "Start a bundle in a cluster group")
public class StartBundleCommand extends BundleCommandSupport {

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster group " + clusterName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        // update the bundle in the cluster group
        String location;
        String key = null;
            Map<String, BundleState> clusterBundles = cluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + clusterName);

            key = selector(clusterBundles);

            if (key == null) {
                System.err.println("Bundle " + key + " not found in cluster group " + clusterName);
            }

            BundleState state = clusterBundles.get(key);
            if (state == null) {
                System.err.println("Bundle " + key + " not found in cluster group " + clusterName);
                return null;
            }
            location = state.getLocation();

            // check if the bundle is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            if (!support.isAllowed(cluster.getName(), Constants.CATEGORY, location, EventType.OUTBOUND)) {
                System.err.println("Bundle location " + location + " is blocked outbound for cluster group " + clusterName);
                return null;
            }

            state.setStatus(BundleEvent.STARTED);
            clusterBundles.put(key, state);

        // broadcast the cluster event
        String[] split = key.split("/");
        ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.STARTED);
        event.setSourceCluster(cluster);
        eventProducer.produce(event);

        return null;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
