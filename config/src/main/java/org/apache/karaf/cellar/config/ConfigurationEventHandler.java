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
package org.apache.karaf.cellar.config;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

/**
 * ConfigurationEventHandler handles received configuration cluster event.
 */
public class ConfigurationEventHandler extends ConfigurationSupport implements EventHandler<ClusterConfigurationEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.configuration.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private ConfigurationAdmin configurationAdmin;
    private CellarSupport cellarSupport;
    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private NodeConfiguration nodeConfiguration;

    @Override
    public void handle(ClusterConfigurationEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR CONFIG: {} switch is OFF, cluster event not handled", SWITCH_ID);
            return;
        }

        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR CONFIG: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }
        Group group = event.getSourceGroup();
        String groupName = group.getName();

        String pid = event.getId();

        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
        List<String> configWhitelist = groupConfig.getInboundConfigurationWhitelist();
        List<String> configBlacklist = groupConfig.getInboundConfigurationBlacklist();
        if (cellarSupport.isAllowed(pid, configWhitelist, configBlacklist)) {

            Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            Properties clusterDictionary = clusterConfigurations.get(pid);
            Configuration conf;
            try {
                conf = configurationAdmin.getConfiguration(pid, null);
                if (event.getType() == ConfigurationEvent.CM_DELETED) {
                    if (conf.getProperties() != null) {
                        // delete the properties
                        conf.delete();
                        deleteStorage(pid);
                    }
                } else {
                    if (clusterDictionary != null) {
                        Dictionary localDictionary = conf.getProperties();
                        if (localDictionary == null) {
                            localDictionary = new Properties();
                        }
                        localDictionary = filter(localDictionary);
                        if (!equals(clusterDictionary, localDictionary)) {
                            conf.update((Dictionary) clusterDictionary);
                            persistConfiguration(configurationAdmin, pid, clusterDictionary);
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("CELLAR CONFIG: failed to read cluster configuration", ex);
            }
        } else {
            LOGGER.warn("CELLAR CONFIG: configuration PID {} is marked BLOCKED INBOUND for cluster group {}", pid, groupName);
        }
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Get the cluster configuration event handler switch.
     *
     * @return the cluster configuration event handler switch.
     */
    @Override
    public Switch getSwitch() {
        boolean status = this.nodeConfiguration.getEnabledEvents().contains(this.getClass().getName());
        if (status) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
    }

    /**
     * Get the cluster event type.
     *
     * @return the cluster configuration event type.
     */
    @Override
    public Class<ClusterConfigurationEvent> getType() {
        return ClusterConfigurationEvent.class;
    }

    /**
     * @return the cellarSupport
     */
    public CellarSupport getCellarSupport() {
        return cellarSupport;
    }

    /**
     * @param cellarSupport the cellarSupport to set
     */
    public void setCellarSupport(CellarSupport cellarSupport) {
        this.cellarSupport = cellarSupport;
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
     * @return the configurationAdmin
     */
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    /**
     * @param configurationAdmin the configurationAdmin to set
     */
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

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
}
