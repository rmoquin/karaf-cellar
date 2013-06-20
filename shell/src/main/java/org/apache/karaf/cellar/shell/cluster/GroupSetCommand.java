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
import org.apache.karaf.cellar.core.control.ManageClusterAction;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.List;

@Command(scope = "cluster", name = "cluster-set", description = "Set the target nodes to a cluster")
public class GroupSetCommand extends ClusterSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "node", description = "The node(s) ID", required = false, multiValued = true)
    List<String> nodes;

    @Override
    protected Object doExecute() throws Exception {
        CellarCluster group = clusterManager.findClusterByName(clusterName);
        if (group == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        return doExecute(ManageClusterAction.SET, clusterName, null, nodes, false);
    }

}