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

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.shell.ClusterCommandSupport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.control.ManageClusterAction;
import org.apache.karaf.cellar.core.control.ManageClusterCommand;
import org.apache.karaf.cellar.core.control.ManageClusterResult;

/**
 * Generic cluster group shell command support.
 */
public abstract class ClusterSupport extends ClusterCommandSupport {

    protected static final String HEADER_FORMAT = "   %-20s   %s";
    protected static final String OUTPUT_FORMAT = "%1s [%-20s] [%s]";

    protected Object doExecute(ManageClusterAction action, String group, CellarCluster source, Collection<String> nodes) throws Exception {
        return doExecute(action, group, source, nodes, true);
    }

    /**
     * Executes the command.
     *
     * @param action the group action to perform.
     * @param cluster the cluster name.
     * @param nodeIds the node IDs.
     * @param suppressOutput true to display command output, false else.
     * @return the Object resulting of the command execution.
     * @throws Exception in case of execution failure.
     */
    protected Object doExecute(ManageClusterAction action, String cluster, CellarCluster source, Collection<String> nodeIds, Boolean suppressOutput) throws Exception {

        ManageClusterCommand command = new ManageClusterCommand(clusterManager.generateId());

        // looking for nodes and check if exist
        Set<Node> recipientList = new HashSet<Node>();
        if (nodeIds != null && !nodeIds.isEmpty()) {
            for (String nodeId : nodeIds) {
                Node node = clusterManager.findNodeById(nodeId);
                if (node == null) {
                    System.err.println("Cluster node " + nodeId + " doesn't exist");
                } else {
                    recipientList.add(node);
                }
            }
        } else {
            recipientList.add(clusterManager.getMasterCluster().getLocalNode());
        }

        if (recipientList.size() < 1) {
            return null;
        }

        command.setDestinations(recipientList);
        command.setAction(action);

        if (cluster != null) {
            command.setClusterName(cluster);
        }

        if (source != null) {
            command.setSourceCluster(source);
        }

        Map<Node, ManageClusterResult> results = executionContext.execute(command);
        if (!suppressOutput) {
            if (results == null || results.isEmpty()) {
                System.out.println("No result received within given timeout");
            } else {
                System.out.println(String.format(HEADER_FORMAT, "Cluster", "Members"));
                for (Node node : results.keySet()) {
                    ManageClusterResult result = results.get(node);
                    if (result != null && result.getClusters()!= null) {
                        for (CellarCluster g : result.getClusters()) {
                            StringBuilder buffer = new StringBuilder();
                            if (!g.listNodes().isEmpty()) {
                                String mark = " ";
                                for (Node member : g.listNodes()) {
                                    buffer.append(member.getId());
                                    if (member.equals(clusterManager.getMasterCluster().getLocalNode())) {
                                        mark = "*";
                                        buffer.append(mark);
                                    }
                                    buffer.append(" ");
                                }
                                System.out.println(String.format(OUTPUT_FORMAT, mark, g.getName(), buffer.toString()));
                            } else System.out.println(String.format(OUTPUT_FORMAT, "", g.getName(), ""));
                        }
                    }
                }
            }
        }
        return null;
    }

}