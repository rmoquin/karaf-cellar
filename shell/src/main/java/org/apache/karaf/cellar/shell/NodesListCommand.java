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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.shell.commands.Command;

import java.util.Set;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

@Command(scope = "cluster", name = "node-list", description = "List the nodes in the cluster")
public class NodesListCommand extends CellarCommandSupport {
    private static final String HEADER_FORMAT = "   %-30s   %-20s   %-5s";
    private static final String OUTPUT_FORMAT = "%1s [%-30s] [%-20s] [%5s]";

    @Override
    protected Object doExecute() throws Exception {
        Set<Node> nodes = clusterManager.listNodes();
        if (nodes != null && !nodes.isEmpty()) {
            System.out.println(String.format(HEADER_FORMAT, "ID", "Host Name", "Port"));
            for (Node node : nodes) {
                String mark = " ";
                if (node.equals(clusterManager.getMasterCluster().getLocalNode())) {
                    mark = "*";
                }
                System.out.println(String.format(OUTPUT_FORMAT, mark, node.getId(), node.getHost(), node.getPort()));
            }
        } else {
            System.err.println("No node found in the cluster");
        }
        return null;
    }

}
