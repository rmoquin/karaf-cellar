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

import java.util.Dictionary;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.event.Event;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

/**
 * ConfigurationEventTask handles received configuration cluster event.
 */
public class ConfigurationEventTask extends Event<ConfigurationTaskResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationEventTask.class);
    private String pid;
    private int type;
    private ConfigurationAdmin configurationAdmin;
    private CellarSupport cellarSupport;
    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private NodeConfiguration nodeConfiguration;
    private final ConfigurationSupport configSupport = new ConfigurationSupport();

    public ConfigurationEventTask() {
    }

    public ConfigurationEventTask(String pid) {
    }

    @Override
    public ConfigurationTaskResult execute() {

        Group group = this.sourceGroup;
        String groupName = group.getName();
        ConfigurationTaskResultImpl result = new ConfigurationTaskResultImpl();
        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
            Set<String> configWhitelist = groupConfig.getInboundConfigurationWhitelist();
            Set<String> configBlacklist = groupConfig.getInboundConfigurationBlacklist();
            if (cellarSupport.isAllowed(pid, configWhitelist, configBlacklist)) {

                Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

                Properties clusterDictionary = clusterConfigurations.get(pid);
                Configuration conf;
                conf = configurationAdmin.getConfiguration(pid, null);
                if (type == ConfigurationEvent.CM_DELETED) {
                    if (conf.getProperties() != null) {
                        // delete the properties
                        conf.delete();
                        configSupport.deleteStorage(pid);
                    }
                } else {
                    if (clusterDictionary != null) {
                        Dictionary localDictionary = conf.getProperties();
                        if (localDictionary == null) {
                            localDictionary = new Properties();
                        }
                        localDictionary = configSupport.filter(localDictionary);
                        if (!configSupport.equals(clusterDictionary, localDictionary)) {
                            conf.update((Dictionary) clusterDictionary);
                            configSupport.persistConfiguration(configurationAdmin, pid, clusterDictionary);
                        }
                    }
                }
            } else {
                LOGGER.warn("CELLAR CONFIG: configuration PID {} is marked BLOCKED INBOUND for cluster group {}", pid, groupName);
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR CONFIG: configuration PID " + pid + " is marked BLOCKED INBOUND for cluster group " + sourceGroup.getName()));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR FEATURES: failed to handle configuration task event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
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

    /**
     * @return the pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * @param pid the pid to set
     */
    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }
}
