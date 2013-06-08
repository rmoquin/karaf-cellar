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
import org.apache.karaf.cellar.core.SynchronizationConfiguration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class HazelcastSynchronizationConfiguration implements SynchronizationConfiguration {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastSynchronizationConfiguration.class);
    private Dictionary<String, Object> properties;
    private ConfigurationAdmin configurationAdmin;
    private String pid;

    public void updated(Dictionary<String, Object> newProperties) {
        this.properties = newProperties;
    }

    /**
     * @return the properties
     */
    @Override
    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    @Override
    public void setProperties(Dictionary<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    @Override
    public void save() {
        Configuration configuration;
        try {
            configuration = this.configurationAdmin.getConfiguration(pid, "?");
            configuration.update(this.properties);
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
}
