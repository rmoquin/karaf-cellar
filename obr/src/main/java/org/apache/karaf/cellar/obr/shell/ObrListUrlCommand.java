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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "obr-list-url", description = "List the OBR URLs in a cluster")
public class ObrListUrlCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Override
    public Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        // get the OBR URLs in a cluster group
        Set<String> clusterUrls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        if (clusterUrls != null) {
            for (String url : clusterUrls) {
                System.out.println(url);
            }
        }

        return null;
    }

}
