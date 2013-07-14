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
package org.apache.karaf.cellar.hazelcast.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.SwitchConfiguration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class HazelcastSwitchConfiguration implements SwitchConfiguration {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastSwitchConfiguration.class);
    private Map<String, Object> properties;
    private ConfigurationAdmin configurationAdmin;
    private String pid = Configurations.NODE_SYNC_RULES_PID;

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    @Override
    public void updated(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public void save() {
        try {
            LOGGER.info("Saving the synchronization configuration.");
            Configuration configuration = this.configurationAdmin.getConfiguration(pid, "?");
            Dictionary<String, Object> dictionary = configuration.getProperties();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                dictionary.put(entry.getKey(), entry.getValue());
            }
            configuration.update(dictionary);
        } catch (IOException ex) {
            LOGGER.error("Error saving configuration " + pid, ex);
        }
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
    
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
