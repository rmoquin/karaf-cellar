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
package org.apache.karaf.cellar.shell.group;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.cellar.core.control.ManageGroupCommand;
import org.apache.karaf.cellar.core.control.ManageGroupResult;
import org.apache.karaf.cellar.shell.ClusterCommandSupport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.Configurations;

/**
 * Generic cluster group shell command support.
 */
public abstract class GroupSupport extends ClusterCommandSupport {
    protected static final String HEADER_FORMAT = "   %-20s   %s";
    protected static final String OUTPUT_FORMAT = "%1s [%-20s] [%s]";

    protected Object doExecute(ManageGroupAction action, String group, Group source, Collection<String> nodes) throws Exception {
        return doExecute(action, group, source, nodes, false);
    }

    /**
     * Executes the command.
     *
     * @param action the group action to perform.
     * @param group the cluster group name.
     * @param nodeNames the node IDs.
     * @param suppressOutput true to display command output, false else.
     * @return the Object resulting of the command execution.
     * @throws Exception in case of execution failure.
     */
    protected Object doExecute(ManageGroupAction action, String group, Group source, Collection<String> nodeNames, Boolean suppressOutput) throws Exception {

        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
        if (source == null) {
            source = super.groupManager.findGroupByName(Configurations.DEFAULT_GROUP_NAME);
        }

        // looking for nodes and check if exist
        Set<Node> recipientList = new HashSet<Node>();
        if (nodeNames != null && !nodeNames.isEmpty()) {
            for (String nodeName : nodeNames) {
                Node node = clusterManager.findNodeByName(nodeName);
                if (node == null) {
                    System.err.println("Cluster node " + nodeName + " doesn't exist");
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

        if (group != null) {
            command.setGroupName(group);
        }

        if (source != null) {
            command.setSourceGroup(source);
        }

        Map<Node, ManageGroupResult> results = executionContext.execute(command);
        if (!suppressOutput) {
            if (results == null || results.isEmpty()) {
                System.out.println("No result received within given timeout");
            } else {
                System.out.println(String.format(HEADER_FORMAT, "Group", "Members"));
                for (Node node : results.keySet()) {
                    ManageGroupResult result = results.get(node);
                    if (result != null && result.getGroups() != null) {
                        printGroups(result.getGroups());
                    }
                }
            }
        }
        return null;
    }

    protected Object printGroups(Set<Group> groups) {
        for (Group g : groups) {
            StringBuilder buffer = new StringBuilder();
            if (g.getNodes() != null && !g.getNodes().isEmpty()) {
                String mark = " ";
                for (Node member : g.getNodes()) {
                    // display only up and running nodes in the cluster
                    if (clusterManager.findNodeByName(member.getName()) != null) {
                        buffer.append(member.getName());
                        if (member.equals(clusterManager.getMasterCluster().getLocalNode())) {
                            mark = "*";
                            buffer.append(mark);
                        }
                        buffer.append(" ");
                    }
                }
                System.out.println(String.format(OUTPUT_FORMAT, mark, g.getName(), buffer.toString()));
            } else {
                System.out.println(String.format(OUTPUT_FORMAT, "", g.getName(), ""));
            }
        }
        return null;
    }
}
