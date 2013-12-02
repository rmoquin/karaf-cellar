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
package org.apache.karaf.cellar.core.command;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Command handler.
 */
public abstract class CommandHandler<C extends Command<R>, R extends DistributedResult> implements EventHandler<C, R> {

    protected NodeConfiguration nodeConfiguration;
    protected GroupManager groupManager;
    protected ClusterManager clusterManager;
    protected CellarSupport cellarSupport = new CellarSupport();
    protected ConfigurationAdmin configAdmin;

    /**
     * /**
     * Get the type of the command.
     *
     * @return the type class.
     */
    @Override
    public abstract Class<C> getType();

    /**
     * Get the switch of the command handler.
     *
     * @return the command handler switch.
     */
    @Override
    public abstract Switch getSwitch();

    /**
     * @return the groupManager
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * @param groupManager the groupManager to set
     */
    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /**
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }

    /**
     * @return the clusterManager
     */
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    /**
     * @param clusterManager the clusterManager to set
     */
    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    /**
     * @return the configAdmin
     */
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    /**
     * @param configAdmin the configAdmin to set
     */
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
