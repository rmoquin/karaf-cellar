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
package org.apache.karaf.cellar.shell.cluster;

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

@Command(scope = "cluster", name = "cluster-delete", description = "Delete a cluster")
public class GroupDeleteCommand extends ClusterSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster group name", required = true, multiValued = false)
    String clusterName;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        // check if the group doesn't contain nodes
        if (!cluster.listNodes().isEmpty()) {
            System.err.println("Cluster " + clusterName  + " is not empty");
            return null;
        }

        clusterManager.deleteCluster(clusterName);

        return null;
    }

}
