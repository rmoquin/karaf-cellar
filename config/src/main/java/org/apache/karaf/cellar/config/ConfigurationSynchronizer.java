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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The ConfigurationSynchronizer is called when Cellar starts or when a node joins a cluster group. The purpose is to
 * synchronize local configurations with the configurations in the cluster groups.
 */
public class ConfigurationSynchronizer extends ConfigurationSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationSynchronizer.class);
    private ConfigurationAdmin configAdmin;
    private GroupManager groupManager;
    private ClusterManager clusterManager;
    private final CellarSupport cellarSupport = new CellarSupport();
    private NodeConfiguration nodeConfiguration;
    private DistributedExecutionContext executionContext;

    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else {
                    LOGGER.warn("CELLAR CONFIG: sync is disabled for cluster group {}", group.getName());
                }
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Pull the configuration from a cluster group to update the local ones.
     *
     * @param group the cluster group where to get the configurations.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR CONFIG: pulling configurations from cluster group {}", groupName);

            Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
            Set<String> configWhitelist = groupConfig.getInboundConfigurationWhitelist();
            Set<String> configBlacklist = groupConfig.getInboundConfigurationBlacklist();
            for (String pid : clusterConfigurations.keySet()) {
                if (cellarSupport.isAllowed(pid, configWhitelist, configBlacklist)) {
                    Dictionary clusterDictionary = clusterConfigurations.get(pid);
                    try {
                        // update the local configuration if needed
                        Configuration localConfiguration = configAdmin.getConfiguration(pid, "?");
                        Dictionary localDictionary = localConfiguration.getProperties();
                        if (localDictionary == null) {
                            localDictionary = new Properties();
                        }

                        if (!equals(localDictionary, clusterDictionary)) {
                            localConfiguration.update(localDictionary);
                        }
                    } catch (IOException ex) {
                        LOGGER.error("CELLAR CONFIG: failed to read local configuration", ex);
                    }
                } else {
                    LOGGER.debug("CELLAR CONFIG: configuration with PID {} is marked BLOCKED INBOUND for cluster group {}", pid, groupName);
                }
            }
        }
    }

    /**
     * Push local configurations to a cluster group.
     *
     * @param group the cluster group where to update the configurations.
     */
    @Override
    public void push(Group group) {

        if (executionContext.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR CONFIG: pushing configurations to cluster group {}", groupName);
            Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
            Configuration[] localConfigurations;
            try {
                localConfigurations = configAdmin.listConfigurations(null);
                for (Configuration localConfiguration : localConfigurations) {
                    String pid = localConfiguration.getPid();
                    // check if the pid is marked as local.
                    Set<String> bundleWhitelist = groupConfig.getOutboundConfigurationWhitelist();
                    Set<String> bundleBlacklist = groupConfig.getOutboundConfigurationBlacklist();

                    if (cellarSupport.isAllowed(pid, bundleWhitelist, bundleBlacklist)) {
                        Dictionary localDictionary = localConfiguration.getProperties();
						localDictionary = filter(localDictionary);
                        // update the configurations in the cluster group
                        clusterConfigurations.put(pid, dictionaryToProperties(localDictionary));
                        // broadcast the cluster event
                        ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
                        event.setSourceGroup(group);
                        event.setSourceNode(groupManager.getNode());
                        event.setType(event.getType());
                        executionContext.execute(event, group.getNodes());
                    } else {
                        LOGGER.debug("CELLAR CONFIG: configuration with PID {} is marked BLOCKED OUTBOUND for cluster group {}", pid, groupName);
                    }
                }
            } catch (IOException ex) {
                LOGGER.error("CELLAR CONFIG: failed to read configuration (IO error)", ex);
            } catch (InvalidSyntaxException ex) {
                LOGGER.error("CELLAR CONFIG: failed to read configuration (invalid filter syntax)", ex);
            }
        }
    }

    /**
     * Check if configuration sync flag is enabled for a cluster group.
     *
     * @param group the cluster group.
     * @return true if the configuration sync flag is enabled for the cluster group, false else.
     */
    @Override
    public boolean isSyncEnabled(Group group) {
        String groupName = group.getName();
        GroupConfiguration groupConfig = this.groupManager.findGroupConfigurationByName(groupName);
        if (groupConfig == null) {
            LOGGER.warn("Cannot check if synchronization is allowed because group {} appears to no longer exist.  Assuming it's not.", groupName);
            return false;
        }
        return groupConfig.isSyncConfiguration();
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
     * @return the nodeconfiguration
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
