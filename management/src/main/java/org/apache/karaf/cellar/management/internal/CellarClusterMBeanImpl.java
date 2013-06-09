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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.control.ManageClusterAction;
import org.apache.karaf.cellar.core.control.ManageClusterCommand;
import org.apache.karaf.cellar.management.CellarClusterMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.HashSet;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;

/**
 * Implementation of the Cellar Group MBean;
 */
public class CellarClusterMBeanImpl extends StandardMBean implements CellarClusterMBean {
    private ClusterManager clusterManager;
    private ExecutionContext executionContext;

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public CellarClusterMBeanImpl() throws NotCompliantMBeanException {
        super(CellarClusterMBean.class);
    }

    @Override
    public void join(String clusterName, String nodeId) throws Exception {
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        Node node = cluster.findNodeById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
        }
        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageClusterCommand command = new ManageClusterCommand(clusterManager.generateId());
        command.setAction(ManageClusterAction.JOIN);
        command.setClusterName(clusterName);
        command.setDestinations(nodes);

        executionContext.execute(command);
    }

    @Override
    public void quit(String clusterName, String nodeId) throws Exception {
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        Node node = cluster.findNodeById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
        }
        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageClusterCommand command = new ManageClusterCommand(cluster.generateId());
        command.setAction(ManageClusterAction.LEAVE);
        command.setClusterName(clusterName);
        command.setDestinations(nodes);
        executionContext.execute(command);
    }

    @Override
    public TabularData getClusters() throws Exception {
        Set<CellarCluster> allClusters = clusterManager.getClusters();

        CompositeType groupType = new CompositeType("Group", "Karaf Cellar cluster group",
                new String[] { "name", "members" },
                new String[] { "Name of the cluster group", "Members of the cluster group" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        TabularType tableType = new TabularType("Groups", "Table of all Karaf Cellar groups", groupType,
                new String[] { "name" });

        TabularData table = new TabularDataSupport(tableType);

        for (CellarCluster cluster : allClusters) {
            StringBuilder members = new StringBuilder();
            for (Node node : cluster.listNodes()) {
                members.append(node.getId());
                members.append(" ");
            }
            CompositeData data = new CompositeDataSupport(groupType,
                    new String[] { "name", "members" },
                    new Object[] { cluster.getName(), members.toString() });
            table.put(data);
        }

        return table;
    }
}
