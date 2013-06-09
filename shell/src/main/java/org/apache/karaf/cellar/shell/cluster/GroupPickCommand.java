/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.shell.cluster;

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageClusterAction;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Command(scope = "cluster", name = "group-pick", description = "Picks a number of nodes from one cluster group and moves them into another")
public class GroupPickCommand extends GroupSupport {

    @Argument(index = 0, name = "sourceClusterName", description = "The source cluster name", required = true, multiValued = false)
    String sourceClusterName;

    @Argument(index = 1, name = "targetClusterName", description = "The destination cluster name", required = true, multiValued = false)
    String targetClusterName;

    @Argument(index = 2, name = "count", description = "The number of nodes to transfer", required = false, multiValued = false)
    int count = 1;

    @Override
    protected Object doExecute() throws Exception {
        CellarCluster sourceCluster = clusterManager.findClusterByName(sourceClusterName);
        if (sourceCluster == null) {
            System.err.println("Source cluster " + sourceClusterName + " doesn't exist");
            return null;
        }
        CellarCluster targetCluster = clusterManager.findClusterByName(targetClusterName);
        if (targetCluster == null) {
            System.err.println("Target cluster group " + targetClusterName + " doesn't exist");
            return null;
        }

        Set<Node> groupMembers = sourceCluster.listNodes();

        if (count > groupMembers.size())
            count = groupMembers.size();

        int i = 0;
        for (Node node : groupMembers) {
            if (i >= count)
                break;
            List<String> recipients = new LinkedList<String>();
            recipients.add(node.getId());
            doExecute(ManageClusterAction.SET, targetClusterName, sourceCluster, recipients);
            i++;
        }

        return doExecute(ManageClusterAction.LIST, null, null, new ArrayList(), false);
    }

}
