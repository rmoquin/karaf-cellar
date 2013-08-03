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

import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.control.*;
import org.apache.karaf.cellar.management.CellarMBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;

/**
 * Implementation of the Cellar Core MBean.
 */
public class CellarMBeanImpl extends StandardMBean implements CellarMBean {
    private BundleContext bundleContext;
    private ClusterManager clusterManager;
    private ExecutionContext executionContext;
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

    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
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
            if (group.getNodes().size() > 1) {
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
    }

    @Override
    public TabularData handlerStatus() throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());

        command.setDestinations(clusterManager.listNodes());
        command.setHandlerName(null);
        command.setStatus(null);

        Map<Node, ManageHandlersResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Handler", "Karaf Cellar cluster event handler",
                new String[] { "node", "handler", "status", "local" },
                new String[] { "Node hosting event handler", "Name of the event handler", "Current status of the event handler", "True if the node is local" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Event Handlers", "Table of Karaf Cellar cluster event handlers",
                compositeType, new String[] { "node", "handler" });
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
                            new String[] { "node", "handler", "status", "local" },
                            new Object[] { node.getId(), handler, status, local });
                    table.put(data);
                }
            }
        }

        return table;
    }

    @Override
    public void handlerStart(String handlerId, String nodeId) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        Set<Node> nodes = new HashSet<Node>();

        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
            }
            nodes.add(node);
        }

        command.setHandlerName(handlerId);
        command.setDestinations(nodes);
        command.setStatus(Boolean.TRUE);
    }

    @Override
    public void handlerStop(String handlerId, String nodeId) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());

        Set<Node> nodes = new HashSet<Node>();
        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
            }
            nodes.add(node);
        }

        command.setHandlerName(handlerId);
        command.setDestinations(nodes);
        command.setStatus(Boolean.FALSE);
    }

    @Override
    public TabularData consumerStatus() throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        command.setStatus(null);

        Map<Node, ConsumerSwitchResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Consumer", "Karaf Cellar cluster event consumer",
                new String[] { "node", "status", "local" },
                new String[] { "Node hosting event consumer", "Current status of the event consumer", "True if the node is local" },
                new OpenType[] { SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Event Consumers", "Table of Karaf Cellar cluster event consumers",
                compositeType, new String[] { "node" });
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Node node : results.keySet()) {
            boolean local = (node.equals(clusterManager.getMasterCluster().getLocalNode()));
            ConsumerSwitchResult consumerSwitchResult = results.get(node);
            CompositeDataSupport data = new CompositeDataSupport(compositeType,
                    new String[] { "node", "status", "local" },
                    new Object[] { node.getId(), consumerSwitchResult.getStatus(), local });
            table.put(data);
        }

        return table;
    }

    @Override
    public void consumerStart(String nodeId) throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());

        Set<Node> nodes = new HashSet<Node>();

        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
            }
            nodes.add(node);
        }

        command.setDestinations(nodes);
        command.setStatus(SwitchStatus.ON);
        executionContext.execute(command);
    }

    @Override
    public void consumerStop(String nodeId) throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());

        Set<Node> nodes = new HashSet<Node>();

        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
            }
            nodes.add(node);
        }

        command.setDestinations(nodes);
        command.setStatus(SwitchStatus.OFF);
        executionContext.execute(command);
    }

    @Override
    public TabularData producerStatus() throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());
        command.setStatus(null);

        Map<Node, ProducerSwitchResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Producer", "Karaf Cellar cluster event producer",
                new String[] { "node", "status", "local" },
                new String[] { "Node hosting event producer", "Current status of the event producer", "True if the node is local" },
                new OpenType[] { SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Event Producers", "Table of Karaf Cellar cluster event producers",
                compositeType, new String[] { "node" });
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Node node : results.keySet()) {
            boolean local = (node.equals(clusterManager.getMasterCluster().getLocalNode()));
            ProducerSwitchResult producerSwitchResult = results.get(node);
            CompositeDataSupport data = new CompositeDataSupport(compositeType,
                    new String[] { "node", "status", "local" },
                    new Object[] { node.getId(), producerSwitchResult.getStatus(), local });
            table.put(data);
        }

        return table;
    }

    @Override
    public void producerStop(String nodeId) throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());

        Set<Node> nodes = new HashSet<Node>();

        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist");
            }
            nodes.add(node);
        }

        command.setDestinations(nodes);
        command.setStatus(SwitchStatus.OFF);
        executionContext.execute(command);
    }

    @Override
    public void producerStart(String nodeId) throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());

        Set<Node> nodes = new HashSet<Node>();

        if (nodeId == null || nodeId.isEmpty()) {
            nodes.add(clusterManager.getMasterCluster().getLocalNode());
        } else {
            Node node = clusterManager.findNodeById(nodeId);
            if (node == null) {
                throw new IllegalArgumentException("Cluster node " + nodeId + " doesn't exist)");
            }
            nodes.add(node);
        }

        command.setDestinations(nodes);
        command.setStatus(SwitchStatus.ON);
        executionContext.execute(command);
    }
}
