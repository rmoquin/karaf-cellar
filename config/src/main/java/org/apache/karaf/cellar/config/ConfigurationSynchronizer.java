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
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import org.apache.karaf.cellar.core.CellarCluster;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The ConfigurationSynchronizer is called when Cellar starts or when a node joins a cluster group.
 * The purpose is to synchronize local configurations with the configurations in the cluster groups.
 */
public class ConfigurationSynchronizer extends ConfigurationSupport implements Synchronizer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationSynchronizer.class);
    private ConfigurationAdmin configurationAdmin;
    
    public ConfigurationSynchronizer() {
        // nothing to do
    }

    public void init() {
        Collection<CellarCluster> clusters = clusterManager.getClusters();
        if (clusters != null && !clusters.isEmpty()) {
            for (CellarCluster cluster : clusters) {
                if (isSyncEnabled(cluster)) {
                    pull(cluster);
                    push(cluster);
                } else {
                    LOGGER.warn("CELLAR CONFIG: sync is disabled for cluster group {}", cluster.getName());
                }
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    @Override
    public boolean synchronize(CellarCluster cluster) {
        this.pull(cluster);
        this.push(cluster);
        return true;
    }

    /**
     * Pull the configuration from a cluster group to update the local ones.
     *
     * @param cluster the cluster group where to get the configurations.
     */
    @Override
    public void pull(CellarCluster cluster) {
        if (cluster != null) {
            String clusterName = cluster.getName();
            LOGGER.debug("CELLAR CONFIG: pulling configurations from cluster group {}", clusterName);

            Map<String, Properties> clusterConfigurations = cluster.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);

            for (String clusterConfiguration : clusterConfigurations.keySet()) {
                if (isAllowed(cluster.getName(), Constants.CATEGORY, clusterConfiguration, EventType.INBOUND)) {
                    Dictionary clusterDictionary = clusterConfigurations.get(clusterConfiguration);
                    try {
                        // update the local configuration if needed
                        Configuration localConfiguration = getConfigurationAdmin().getConfiguration(clusterConfiguration, null);
                        Dictionary localDictionary = localConfiguration.getProperties();
                        if (localDictionary == null) {
                            localDictionary = new Properties();
                        }

                        localDictionary = filter(localDictionary);
                        if (!equals(localDictionary, clusterDictionary)) {
                            localConfiguration.update(localDictionary);
                            persistConfiguration(getConfigurationAdmin(), clusterConfiguration, localDictionary);
                        }
                    } catch (IOException ex) {
                        LOGGER.error("CELLAR CONFIG: failed to read local configuration", ex);
                    }
                } else {
                    LOGGER.warn("CELLAR CONFIG: configuration with PID {} is marked BLOCKED INBOUND for cluster group {}", clusterConfiguration, clusterName);
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
    public void push(CellarCluster cluster) {

        // check if the producer is ON
        if (cluster.emitsEvents()) {
            LOGGER.warn("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }

        String clusterName = cluster.getName();
        LOGGER.debug("CELLAR CONFIG: pushing configurations to cluster group {}", clusterName);
        Map<String, Properties> clusterConfigurations = cluster.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);

        Configuration[] localConfigurations;
        try {
            localConfigurations = getConfigurationAdmin().listConfigurations(null);
            for (Configuration localConfiguration : localConfigurations) {
                String pid = localConfiguration.getPid();
                // check if the pid is marked as local.
                if (isAllowed(cluster.getName(), Constants.CATEGORY, pid, EventType.OUTBOUND)) {
                    Dictionary localDictionary = localConfiguration.getProperties();
                    localDictionary = filter(localDictionary);
                    // update the configurations in the cluster group
                    clusterConfigurations.put(pid, dictionaryToProperties(localDictionary));
                    // broadcast the cluster event
                    ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
                    event.setSourceCluster(cluster);
                    cluster.produce(event);
                } else {
                    LOGGER.warn("CELLAR CONFIG: configuration with PID {} is marked BLOCKED OUTBOUND for cluster group {}", pid, clusterName);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("CELLAR CONFIG: failed to read configuration (IO error)", ex);
        } catch (InvalidSyntaxException ex) {
            LOGGER.error("CELLAR CONFIG: failed to read configuration (invalid filter syntax)", ex);
        }
    }

    /**
     * Check if configuration sync flag is enabled for a cluster group.
     *
     * @param cluster the cluster group.
     * @return true if the configuration sync flag is enabled for the cluster group, false else.
     */
    @Override
    public Boolean isSyncEnabled(CellarCluster cluster) {
        String clusterName = cluster.getName();

        String propertyKey = clusterName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
        String propertyValue = (String) super.synchronizationConfiguration.getProperty(propertyKey);
        return Boolean.parseBoolean(propertyValue);
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
}
