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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * LocalConfigurationListener is listening for local configuration changes.
 * When a local configuration change occurs, this listener updates the cluster and broadcasts a cluster config event.
 */
public class LocalConfigurationListener extends ConfigurationSupport implements ConfigurationListener {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalConfigurationListener.class);
    private ConfigurationAdmin configurationAdmin;
    private GroupManager groupManager;
    private ClusterManager clusterManager;
    private EventProducer eventProducer;
    private CellarSupport cellarSupport;

    /**
     * Callback method called when a local configuration changes.
     *
     * @param event the local configuration event.
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }
        String pid = event.getPid();

        Dictionary localDictionary = null;
        if (event.getType() != ConfigurationEvent.CM_DELETED) {
            try {
                Configuration conf = configurationAdmin.getConfiguration(pid, null);
                localDictionary = conf.getProperties();
            } catch (Exception e) {
                LOGGER.error("CELLAR CONFIG: can't retrieve configuration with PID {}", pid, e);
                return;
            }
        }

        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(group.getName());
                Set<String> bundleWhitelist = groupConfig.getOutboundConfigurationWhitelist();
                Set<String> bundleBlacklist = groupConfig.getOutboundConfigurationBlacklist();
                // check if the pid is allowed for outbound.
                if (cellarSupport.isAllowed(pid, bundleWhitelist, bundleBlacklist)) {
                    Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group.getName());
                    try {
                        if (event.getType() == ConfigurationEvent.CM_DELETED) {
                            // update the configurations in the cluster group
                            clusterConfigurations.remove(pid);
                            // broadcast the cluster event
                            ClusterConfigurationEvent clusterConfigurationEvent = new ClusterConfigurationEvent(pid);
                            clusterConfigurationEvent.setType(ConfigurationEvent.CM_DELETED);
                            clusterConfigurationEvent.setSourceGroup(group);
                            clusterConfigurationEvent.setSourceNode(clusterManager.getMasterCluster().getLocalNode());
                            eventProducer.produce(clusterConfigurationEvent);
                        } else {
                            localDictionary = filter(localDictionary);
                            Properties distributedDictionary = clusterConfigurations.get(pid);
                            if (!equals(localDictionary, distributedDictionary)) {
                                // update the configurations in the cluster group
                                clusterConfigurations.put(pid, dictionaryToProperties(localDictionary));
                                ClusterConfigurationEvent clusterConfigurationEvent = new ClusterConfigurationEvent(pid);
                                clusterConfigurationEvent.setType(ConfigurationEvent.CM_UPDATED);
                                clusterConfigurationEvent.setSourceGroup(group);
                                clusterConfigurationEvent.setSourceNode(clusterManager.getMasterCluster().getLocalNode());
                                eventProducer.produce(clusterConfigurationEvent);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("CELLAR CONFIG: failed to update configuration with PID {} in the cluster group {} {}", pid, group.getName(), e);
                    }
                } else {
                    LOGGER.debug("CELLAR CONFIG: configuration with PID {} is marked BLOCKED OUTBOUND for cluster group {} {}", pid, group.getName());
                }
            }
        }
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
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
}
