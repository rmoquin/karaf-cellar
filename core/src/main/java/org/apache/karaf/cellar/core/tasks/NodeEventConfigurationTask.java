/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.karaf.cellar.core.tasks;

import org.apache.karaf.cellar.core.command.DistributedTask;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.control.SwitchType;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class NodeEventConfigurationTask extends DistributedTask<NodeEventConfigurationResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(NodeEventConfigurationTask.class);
    private SwitchStatus status = null;
    private SwitchType type = null;

    public NodeEventConfigurationTask() {
    }

    public NodeEventConfigurationTask(SwitchStatus status, SwitchType type) {
        this.status = status;
        this.type = type;
    }

    @Override
    public NodeEventConfigurationResult execute() throws Exception {
        NodeEventConfigurationResultImpl result = new NodeEventConfigurationResultImpl();
        try {
            result.setSwitchStatus(status);
            result.setSwitchType(type);
//        if (Thread.currentThread().isInterrupted()) {
//            return 0;
//        }
            LOGGER.info("Starting execution of the manage group task received from node {}", getSourceNode().getName());

            ConfigurationAdmin configAdmin = super.getService(ConfigurationAdmin.class);
            //Group manager really isn't where I should be storing node stuff, but it happens to be currently.
            GroupManager groupManager = super.getService(GroupManager.class);
            //This stuff will be needed once more than one node is supported.
//            ClusterManager clusterManager = super.getService(ClusterManager.class);
//            CellarCluster cellarCluster = clusterManager.getMasterCluster();
//            Node node = cellarCluster.getLocalNode();
            NodeConfiguration nodeConfiguration = groupManager.getNodeConfiguration();
            if (SwitchType.CONSUMER.equals(this.type)) {
                nodeConfiguration.setConsumer(SwitchStatus.ON.equals(this.status));
            } else {
                nodeConfiguration.setProducer(SwitchStatus.ON.equals(this.status));
            }
            Configuration configuration = configAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName());
            configuration.update(nodeConfiguration.getProperties());
            result.setSwitchStatus(this.status);
            result.setSwitchType(this.type);
            result.setSuccessful(true);
        } catch (Exception ex) {
            LOGGER.error("Task wasn't processed for some reason.", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    @Override
    public Node getSourceNode() {
        return getSourceNode();
    }

    @Override
    public void setSourceNode(Node sourceNode) {
        this.setSourceNode(sourceNode);
    }

    @Override
    public Group getSourceGroup() {
        return getSourceGroup();
    }

    @Override
    public void setSourceGroup(Group sourceGroup) {
        this.setSourceGroup(sourceGroup);
    }

    /**
     * @return the status
     */
    public SwitchStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(SwitchStatus status) {
        this.status = status;
    }

    /**
     * @return the type
     */
    public SwitchType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SwitchType type) {
        this.type = type;
    }
}
