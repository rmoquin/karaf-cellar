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
package org.apache.karaf.cellar.shell.producer;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.SwitchStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.tasks.NodeEventConfigurationResult;
import org.apache.karaf.cellar.core.tasks.NodeEventConfigurationTask;

/**
 * Generic cluster event producer shell command support.
 */
public abstract class ProducerSupport extends CellarCommandSupport {

    protected static final String HEADER_FORMAT = "   %-30s   %-5s";
    protected static final String OUTPUT_FORMAT = "%1s [%-30s] [%-5s]";

    private DistributedExecutionContext executionContext;

    protected Object doExecute(List<String> nodeNames, SwitchStatus status) throws Exception {

        NodeEventConfigurationTask command = new NodeEventConfigurationTask();

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
            if (status == null) {
                // in case of status display, select all nodes
                recipientList = clusterManager.listNodes();
            } else {
                // in case of status change, select only the local node
                recipientList.add(clusterManager.getMasterCluster().getLocalNode());
            }
        }

        if (recipientList.size() < 1) {
            return null;
        }

        command.setStatus(status);

        Map<Node, NodeEventConfigurationResult> results = executionContext.executeAndWait(command, recipientList);
        if (results == null || results.isEmpty()) {
            System.out.println("No result received within given timeout");
        } else {
            System.out.println(String.format(HEADER_FORMAT, "Node", "Status"));
            for (Node node : results.keySet()) {
                String local = " ";
                if (node.equals(clusterManager.getMasterCluster().getLocalNode())) {
                    local = "*";
                }
                NodeEventConfigurationResult result = results.get(node);
                String statusString = "OFF";
                if (SwitchStatus.ON.equals(result.getSwitchStatus())) {
                    statusString = "ON";
                }
                System.out.println(String.format(OUTPUT_FORMAT, local, node.getName(), statusString));
            }
        }
        return null;
    }

    /**
     * @return the executionContext
     */
    public DistributedExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @param executionContext the executionContext to set
     */
    public void setExecutionContext(DistributedExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

}
