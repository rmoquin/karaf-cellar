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

import com.hazelcast.core.IMap;
import java.text.MessageFormat;
import java.util.Collection;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.karaf.cellar.config.shell.ConfigurationAction;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * ConfigurationEventHandler handles received configuration cluster event.
 */
public class ConfigurationEventHandler extends CommandHandler<ClusterConfigurationEvent, ConfigurationTaskResult> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.configuration.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private ConfigurationSupport configurationSupport;
    private ConfigurationAdmin configAdmin;

    @Override
    public ConfigurationTaskResult execute(ClusterConfigurationEvent command) {

        ConfigurationTaskResult result = new ConfigurationTaskResult(command.getId());

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR CONFIG: {} switch is OFF, cluster event not handled", SWITCH_ID);
            return result;
        }

        Group group = command.getSourceGroup();
        String sourceGroupName = group.getName();
        String pid = command.getId();

        // check if the node is local
        if (!groupManager.isLocalGroup(sourceGroupName)) {
            result.setThrowable(new CommandExecutionException(MessageFormat.format("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName)));
            LOGGER.warn("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName);
            result.setSuccessful(false);
            return result;
        }

        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroupName);
            Set<String> configWhitelist = groupConfig.getInboundConfigurationWhitelist();
            Set<String> configBlacklist = groupConfig.getInboundConfigurationBlacklist();
            if (configurationSupport.isAllowed(pid, configWhitelist, configBlacklist)) {
                ConfigurationAction commandType = command.getType();
                if (ConfigurationAction.DELETE.equals(commandType)) {
                    Configuration[] localConfigurations = configAdmin.listConfigurations("(service.pid=" + pid + ")");
                    if (localConfigurations != null && localConfigurations.length > 0) {
                        localConfigurations[0].delete();
                    }
                } else {
                    Configuration conf = configAdmin.getConfiguration(pid, "?");
                    Dictionary localDictionary = conf.getProperties();
                    if (localDictionary == null) {
                        localDictionary = new Properties();
                    }
                    localDictionary = configurationSupport.filter(localDictionary);
                    String key = command.getPropertyName();
                    Object value = command.getPropertyValue();
                    if (ConfigurationAction.PROP_APPEND.equals(commandType)) {
                        Object currentValue = localDictionary.get(key);
                        if (currentValue == null) {
                            localDictionary.put(key, value);
                        } else if (currentValue instanceof Collection) {
                            Set values = new HashSet();
                            values.addAll((Collection) currentValue);
                            values.add(value);
                            localDictionary.put(key, values);
                        } else {
                            Set values = new HashSet();
                            values.add(currentValue);
                            values.add(value);
                            localDictionary.put(key, values);
                        }
                    } else if (ConfigurationAction.PROP_DELETE.equals(commandType)) {
                        localDictionary.remove(key);
                    } else if (ConfigurationAction.PROP_SET.equals(commandType)) {
                        localDictionary.put(key, value);
                    } else if (ConfigurationAction.SYNC.equals(commandType)) {
                        IMap<String, Properties> clusterConfigurations = (IMap<String, Properties>) clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group.getName());
                        Properties clusterProperties = clusterConfigurations.get(pid);
                        conf.update(configurationSupport.propertiesToDictionary(clusterProperties));
                    } else {
                        LOGGER.debug("CELLAR CONFIG: configuration PID {} is marked BLOCKED INBOUND for cluster group {}", pid, sourceGroupName);
                        result.setSuccessful(false);
                        result.setThrowable(new IllegalStateException("CELLAR CONFIG: Unrecognized configuration event type: " + commandType));
                    }
                    conf.update((Dictionary) localDictionary);
                }
            } else {
                LOGGER.debug("CELLAR CONFIG: configuration PID {} is marked BLOCKED INBOUND for cluster group {}", pid, sourceGroupName);
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR CONFIG: configuration PID " + pid + " is marked BLOCKED INBOUND for cluster group " + sourceGroupName));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR CONFIG: failed to execute cluster configuration command event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        boolean status = nodeConfiguration.getEnabledEvents().contains(this.getType().getName());
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
     * @return the configurationSupport
     */
    public ConfigurationSupport getConfigurationSupport() {
        return configurationSupport;
    }

    /**
     * @param configurationSupport the configurationSupport to set
     */
    public void setConfigurationSupport(ConfigurationSupport configurationSupport) {
        this.configurationSupport = configurationSupport;
    }
}
