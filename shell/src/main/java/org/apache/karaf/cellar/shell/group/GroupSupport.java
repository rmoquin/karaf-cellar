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
import org.apache.karaf.cellar.shell.ClusterCommandSupport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.command.CommandExecutionContext;
import org.apache.karaf.cellar.core.tasks.GroupTaskResult;
import org.apache.karaf.cellar.core.tasks.ManageGroupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic cluster group shell command support.
 */
public abstract class GroupSupport extends ClusterCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupSupport.class);
    protected static final String HEADER_FORMAT = "   %-20s   %s";
    protected static final String OUTPUT_FORMAT = "%1s [%-20s] [%s]";
    private CommandExecutionContext commandExecutionContext;

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
    protected Object doExecute(ManageGroupAction action, String group, Group source, Collection<String> nodeNames, Boolean suppressOutput) throws Exception {;

        ManageGroupTask command = new ManageGroupTask();
        if (source == null) {
            source = super.groupManager.findGroupByName(Configurations.DEFAULT_GROUP_NAME);
            command.setSourceGroup(source);
        }
        command.setAction(action);
        command.setSourceNode(groupManager.getNode());

        // looking for nodes and check if exist
        Set<Node> recipientList = new HashSet<Node>();
        if (nodeNames != null && !nodeNames.isEmpty()) {
            for (String nodeName : nodeNames) {
                Node node = clusterManager.findNodeByName(nodeName);
                //If null, see if it's the node id.
                if (node == null) {
                    node = clusterManager.findNodeById(nodeName);
                }
                if (node == null) {
                    System.err.println("Cluster node " + nodeName + " doesn't exist and won't be included in this command execution.");
                } else {
                    recipientList.add(node);
                }
            }
        } else {
            recipientList.add(clusterManager.getMasterCluster().getLocalNode());
        }

        if (recipientList.size() < 1) {
            System.out.println("At least one destination node must be specified.");
            return null;
        }

        if (group != null) {
            command.setGroupName(group);
        }

        Map<Node, Future<GroupTaskResult>> future = commandExecutionContext.execute(command, recipientList);
        System.out.println(String.format(HEADER_FORMAT, "Group", "Members"));
        for (Map.Entry<Node, Future<GroupTaskResult>> entry : future.entrySet()) {
            Node node = entry.getKey();
            try {
                GroupTaskResult result = entry.getValue().get(5, TimeUnit.SECONDS);
                if (!suppressOutput) {
                    if (result != null && result.getGroups() != null) {
                        printGroups(result.getGroups());
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Error during execution of command {} ", command, ex);
                System.out.println("Node " + node.getName() + " responsed with an error, check logs for details.");
            }
        }
        System.out.println("Completed.");
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

    /**
     * @return the commandExecutionContext
     */
    public CommandExecutionContext getCommandExecutionContext() {
        return commandExecutionContext;
    }

    /**
     * @param commandExecutionContext the commandExecutionContext to set
     */
    public void setCommandExecutionContext(CommandExecutionContext commandExecutionContext) {
        this.commandExecutionContext = commandExecutionContext;
    }
}
