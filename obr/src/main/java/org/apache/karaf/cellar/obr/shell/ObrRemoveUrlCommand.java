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
package org.apache.karaf.cellar.obr.shell;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.obr.ClusterObrUrlEvent;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleInfo;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "obr-remove-url", description = "Remove a repository URL from the distributed OBR service.")
public class ObrRemoveUrlCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name.", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "url", description = "The repository URL to remove from the OBR service.", required = true, multiValued = false)
    String url;

    @Override
    public Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster group " + clusterName + " doesn't exist.");
            return null;
        }

        // check if the producer is ON
        if (!cluster.emitsEvents()) {
            System.err.println("Cluster event producer is OFF for this node");
            return null;
        }

        // check if the URL is allowed
        if (!isAllowed(cluster, Constants.URLS_CONFIG_CATEGORY, url, EventType.OUTBOUND)) {
            System.err.println("OBR URL " + url + " is blocked outbound");
            return null;
        }

        // remove URL from the distributed map
        Set<String> urls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        urls.remove(url);
        // remove bundles from the distributed map
        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        synchronized(obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(),resource.getSymbolicName(), resource.getVersion().toString());
                bundles.remove(info);
            }
            obrService.removeRepository(url);
        }

        // create an event and produce it
        ClusterObrUrlEvent event = new ClusterObrUrlEvent(url, Constants.URL_REMOVE_EVENT_TYPE);
        event.setSourceCluster(cluster);
        cluster.produce(event);

        return null;
    }
}
