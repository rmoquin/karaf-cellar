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

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

@Command(scope = "cluster", name = "feature-install", description = "Install a feature in a cluster group")
public class InstallFeatureCommand extends FeatureCommandSupport {

    @Option(name = "-c", aliases = { "--no-clean" }, description = "Do not uninstall bundles on failure", required = false, multiValued = false)
    boolean noClean;

    @Option(name = "-r", aliases = { "--no-auto-refresh" }, description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "feature", description = "The feature name", required = true, multiValued = false)
    String feature;

    @Argument(index = 2, name = "version", description = "The feature version", required = false, multiValued = false)
    String version;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (cluster.emitsEvents()) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        // check if the feature exists in the map
        if (!featureExists(clusterName, feature, version)) {
            if (version != null)
                System.err.println("Feature " + feature + "/" + version + " doesn't exist in the cluster " + clusterName);
            else System.err.println("Feature " + feature + " doesn't exist in the cluster " + clusterName);
            return null;
        }

        // check if the outbound event is allowed
        if (!isAllowed(cluster, Constants.FEATURES_CATEGORY, feature, EventType.OUTBOUND)) {
            System.err.println("Feature " + feature + " is blocked outbound for cluster " + clusterName);
            return null;
        }

        // update the features in the cluster group
        updateFeatureStatus(clusterName, feature, version, true);

        // broadcast the cluster event
        ClusterFeaturesEvent event = new ClusterFeaturesEvent(feature, version, noClean, noRefresh, FeatureEvent.EventType.FeatureInstalled);
        event.setSourceCluster(cluster);
        cluster.produce(event);

        return null;
    }
}
