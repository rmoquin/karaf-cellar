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

import java.util.ArrayList;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupActions;
import org.apache.karaf.cellar.management.CellarGroupMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.tasks.ManageGroupTask;

/**
 * Implementation of the Cellar Group MBean;
 */
public class CellarGroupMBeanImpl extends StandardMBean implements CellarGroupMBean {
    private ClusterManager clusterManager;
    private DistributedExecutionContext commandExecutionContext;
    private GroupManager groupManager;

    public CellarGroupMBeanImpl() throws NotCompliantMBeanException {
        super(CellarGroupMBean.class);
    }

    @Override
    public void create(String name) throws Exception {
        Group group = groupManager.findGroupByName(name);
        if (group != null) {
            throw new IllegalArgumentException("Cluster group " + name + " already exists");
        }
        groupManager.createGroup(name);
    }

    @Override
    public void delete(String name) throws Exception {
        Group g = groupManager.findGroupByName(name);
        List<String> nodes = new ArrayList<String>();
        if (g.getNodes() != null && !g.getNodes().isEmpty()) {
            for (Node n : g.getNodes()) {
                nodes.add(n.getName());
            }
            ManageGroupTask command = new ManageGroupTask();
            command.setAction(ManageGroupActions.QUIT);
            command.setDestinationGroup(name);
            Set<Node> recipientList = clusterManager.listNodes(nodes);
            commandExecutionContext.execute(command, recipientList);
        }
        groupManager.deregisterNodeFromAllGroups();
    }

    @Override
    public void join(String groupName, String nodeName) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        Node node = clusterManager.findNodeByName(nodeName);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
        }
        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageGroupTask command = new ManageGroupTask();
        command.setAction(ManageGroupActions.JOIN);
        command.setDestinationGroup(groupName);

        commandExecutionContext.execute(command, nodes);
    }

    @Override
    public void quit(String groupName, String nodeName) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        Node node = clusterManager.findNodeByName(nodeName);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
        }
        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageGroupTask command = new ManageGroupTask();
        command.setAction(ManageGroupActions.QUIT);
        command.setDestinationGroup(groupName);
        commandExecutionContext.execute(command, nodes);
    }

    @Override
    public TabularData getGroups() throws Exception {
        Set<Group> allGroups = groupManager.listAllGroups();

        CompositeType groupType = new CompositeType("Group", "Karaf Cellar cluster group",
                new String[] { "name", "members" },
                new String[] { "Name of the cluster group", "Members of the cluster group" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        TabularType tableType = new TabularType("Groups", "Table of all Karaf Cellar groups", groupType,
                new String[] { "name" });

        TabularData table = new TabularDataSupport(tableType);

        for (Group group : allGroups) {
            StringBuilder members = new StringBuilder();
            for (Node node : group.getNodes()) {
                // display only up and running nodes in the cluster
                if (clusterManager.findNodeByName(node.getName()) != null) {
                    members.append(node.getName());
                    members.append(" ");
                }
            }
            CompositeData data = new CompositeDataSupport(groupType,
                    new String[] { "name", "members" },
                    new Object[] { group.getName(), members.toString() });
            table.put(data);
        }

        return table;
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    /**
     * @return the commandExecutionContext
     */
    public DistributedExecutionContext getCommandExecutionContext() {
        return commandExecutionContext;
    }

    /**
     * @param commandExecutionContext the commandExecutionContext to set
     */
    public void setCommandExecutionContext(DistributedExecutionContext commandExecutionContext) {
        this.commandExecutionContext = commandExecutionContext;
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }
}
