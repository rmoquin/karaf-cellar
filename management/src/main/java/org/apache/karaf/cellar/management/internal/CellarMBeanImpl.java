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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.management.CellarMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.control.ManageHandlersCommand;
import org.apache.karaf.cellar.core.control.ManageHandlersResult;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.control.SwitchType;
import org.apache.karaf.cellar.core.control.NodeConfigurationCommand;
import org.apache.karaf.cellar.core.control.NodeConfigurationResult;

/**
 * Implementation of the Cellar Core MBean.
 */
public class CellarMBeanImpl extends StandardMBean implements CellarMBean {

    private BundleContext bundleContext;
    private ClusterManager clusterManager;
    private DistributedExecutionContext executionContext;
    private GroupManager groupManager;

    public CellarMBeanImpl() throws NotCompliantMBeanException {
        super(CellarMBean.class);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public DistributedExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(DistributedExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public void sync() throws Exception {
        Set<Group> localGroups = groupManager.listLocalGroups();
        for (Group group : localGroups) {
            try {
                ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
                if (serviceReferences != null && serviceReferences.length > 0) {
                    for (ServiceReference ref : serviceReferences) {
                        Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                        if (synchronizer.isSyncEnabled(group)) {
                            synchronizer.pull(group);
                            synchronizer.push(group);
                        }
                        bundleContext.ungetService(ref);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // ignore
            }
        }
    }

    @Override
    public TabularData handlerStatus() throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());

        Set<Node> nodes = clusterManager.listNodes();
        command.setHandlerName(null);
        command.setStatus(null);

        Map<Node, ManageHandlersResult> results = executionContext.executeAndWait(command, nodes);

        CompositeType compositeType = new CompositeType("Event Handler", "Karaf Cellar cluster event handler",
                new String[]{"node", "handler", "status", "local"},
                new String[]{"Node hosting event handler", "Name of the event handler", "Current status of the event handler", "True if the node is local"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN});
        TabularType tableType = new TabularType("Event Handlers", "Table of Karaf Cellar cluster event handlers",
                compositeType, new String[]{"node", "handler"});
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Map.Entry<Node, ManageHandlersResult> handlersResultEntry : results.entrySet()) {
            Node node = handlersResultEntry.getKey();
            ManageHandlersResult result = handlersResultEntry.getValue();
            if (result != null && result.getHandlers() != null) {
                for (Map.Entry<String, String> handlerEntry : result.getHandlers().entrySet()) {
                    String handler = handlerEntry.getKey();
                    String status = handlerEntry.getValue();
                    boolean local = (node.equals(clusterManager.getMasterCluster().getLocalNode()));
                    CompositeDataSupport data = new CompositeDataSupport(compositeType,
                            new String[]{"node", "handler", "status", "local"},
                            new Object[]{node.getName(), handler, status, local});
                    table.put(data);
                }
            }
        }

        return table;
    }

    @Override
    public void handlerStart(String handlerId, String nodeName) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());

        Node node;
        if (nodeName == null || nodeName.isEmpty()) {
            node = clusterManager.getMasterCluster().getLocalNode();
        } else {
            node = clusterManager.findNodeByName(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
            }
        }

        command.setHandlerName(handlerId);
        command.setStatus(Boolean.TRUE);
        command.setSourceNode(groupManager.getNode());
        executionContext.execute(command, node);
    }

    @Override
    public void handlerStop(String handlerId, String nodeName) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());

        Node node;
        if (nodeName == null || nodeName.isEmpty()) {
            node = clusterManager.getMasterCluster().getLocalNode();
        } else {
            node = clusterManager.findNodeByName(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
            }
        }

        command.setHandlerName(handlerId);
        command.setStatus(Boolean.FALSE);
        command.setSourceNode(groupManager.getNode());
        executionContext.execute(command, node);
    }

    @Override
    public TabularData consumerStatus() throws Exception {
        NodeConfigurationCommand command = new NodeConfigurationCommand(clusterManager.generateId());
        command.setStatus(null);

        CompositeType compositeType = new CompositeType("Event Consumer", "Karaf Cellar cluster event consumer",
                new String[]{"node", "status", "local"},
                new String[]{"Node hosting event consumer", "Current status of the event consumer", "True if the node is local"},
                new OpenType[]{SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN});
        TabularType tableType = new TabularType("Event Consumers", "Table of Karaf Cellar cluster event consumers",
                compositeType, new String[]{"node"});
        TabularDataSupport table = new TabularDataSupport(tableType);
        this.getNodeStatuses(SwitchType.INBOUND, table, compositeType);
        return table;
    }

    @Override
    public TabularData producerStatus() throws Exception {
        CompositeType compositeType = new CompositeType("Event Producer", "Karaf Cellar cluster event producer",
                new String[]{"node", "status", "local"},
                new String[]{"Node hosting event producer", "Current status of the event producer", "True if the node is local"},
                new OpenType[]{SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN});
        TabularType tableType = new TabularType("Event Producers", "Table of Karaf Cellar cluster event producers",
                compositeType, new String[]{"node"});
        TabularDataSupport table = new TabularDataSupport(tableType);
        this.getNodeStatuses(SwitchType.OUTBOUND, table, compositeType);

        return table;
    }

    @Override
    public void consumerStart(String nodeName) throws Exception {
        start(nodeName, SwitchType.INBOUND);
    }

    @Override
    public void producerStart(String nodeName) throws Exception {
        start(nodeName, SwitchType.OUTBOUND);
    }

    @Override
    public void consumerStop(String nodeName) throws Exception {
        stop(nodeName, SwitchType.INBOUND);
    }

    @Override
    public void producerStop(String nodeName) throws Exception {
        stop(nodeName, SwitchType.OUTBOUND);
    }

    public Map<Node, NodeConfigurationResult> changeNodeStatus(SwitchType switchType, SwitchStatus switchStatus, String nodeName) throws Exception {
        NodeConfigurationCommand command = new NodeConfigurationCommand(clusterManager.generateId(), switchStatus, switchType);

        Node node;
        if (nodeName == null || nodeName.isEmpty()) {
            node = clusterManager.getMasterCluster().getLocalNode();
        } else {
            node = clusterManager.findNodeByName(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
            }
        }

        return this.executionContext.execute(command, node);
    }

    public TabularDataSupport getNodeStatuses(SwitchType switchType, TabularDataSupport table, CompositeType compositeType) throws Exception {
        NodeConfigurationCommand command = new NodeConfigurationCommand(clusterManager.generateId(), null, switchType);
        Set<Node> nodes = clusterManager.listNodes();
        Map<Node, NodeConfigurationResult> results = this.executionContext.execute(command, nodes);
        for (Node node : results.keySet()) {
            boolean local = (node.equals(clusterManager.getMasterCluster().getLocalNode()));
            NodeConfigurationResult nodeConfigurationResult = results.get(node);
            CompositeDataSupport data = new CompositeDataSupport(compositeType,
                    new String[]{"node", "status", "local"},
                    new Object[]{node.getName(), nodeConfigurationResult.getSwitchStatus(), local});
            table.put(data);
        }
        return table;
    }

    private Map<Node, ManageHandlersResult> start(String nodeName, SwitchType switchTyoe) throws Exception {
        NodeConfigurationCommand command = new NodeConfigurationCommand(clusterManager.generateId(), SwitchStatus.ON, switchTyoe);

        Node node;
        if (nodeName == null || nodeName.isEmpty()) {
            node = clusterManager.getMasterCluster().getLocalNode();
        } else {
            node = clusterManager.findNodeByName(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
            }
        }

        command.setSourceNode(groupManager.getNode());
        return executionContext.execute(command, node);
    }

    private Map<Node, ManageHandlersResult> stop(String nodeName, SwitchType switchTyoe) throws Exception {
        NodeConfigurationCommand command = new NodeConfigurationCommand(clusterManager.generateId(), SwitchStatus.OFF, switchTyoe);

        Node node;
        if (nodeName == null || nodeName.isEmpty()) {
            node = clusterManager.getMasterCluster().getLocalNode();
        } else {
            node = clusterManager.findNodeByName(nodeName);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeName + " doesn't exist");
            }
        }

        command.setSourceNode(groupManager.getNode());
        return executionContext.execute(command, node);
    }
}
