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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.List;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.control.ManageClusterAction;

@Command(scope = "cluster", name = "group-join", description = "Join serveral node(s) to a cluster")
public class GroupJoinCommand extends GroupSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "node", description = "The node(s) ID", required = true, multiValued = true)
    List<String> nodes;

    @Override
    protected Object doExecute() throws Exception {
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster group " + clusterName + " doesn't exist");
            return null;
        }

        return doExecute(ManageClusterAction.JOIN, clusterName, null, nodes,false);
    }

}
