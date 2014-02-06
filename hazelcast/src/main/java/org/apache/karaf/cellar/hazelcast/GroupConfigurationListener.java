/*
 * Copyright 2014 The Apache Software Foundation.
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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import java.util.Dictionary;
import org.apache.karaf.cellar.hazelcast.internal.GroupConfigurationImpl;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

/**
 *
 * @author Ryan
 */
public class GroupConfigurationListener implements EntryListener<String, Object> {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GroupConfigurationListener.class);
    ConfigurationAdmin configAdmin;

    public GroupConfigurationListener(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    @Override
    public void entryAdded(EntryEvent<String, Object> event) {
        String key = event.getKey();
        Object value = event.getValue();
        String groupName = event.getName();

        try {
            Configuration configuration = this.getGroupConfiguration(groupName);
            if (configuration != null) {
                configuration.getProperties().put(key, value);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred while attempt to add a key {} with value {} of local group {} ", key, value, groupName, ex);
        }
    }

    @Override
    public void entryRemoved(EntryEvent<String, Object> event) {
        String key = event.getKey();
        String groupName = event.getName();

        try {
            Configuration configuration = this.getGroupConfiguration(groupName);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                configuration.update(properties);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred while attempt to remove key {} of local group {} ", key, groupName, ex);
        }
    }

    @Override
    public void entryUpdated(EntryEvent<String, Object> event) {
        String key = event.getKey();
        Object value = event.getValue();
        String groupName = event.getName();

        try {
            Configuration configuration = this.getGroupConfiguration(groupName);
            if (configuration != null) {
                configuration.getProperties().put(key, value);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred while attempt to update value {} for key {} of local group {} ", value, key, groupName, ex);
        }
    }

    @Override
    public void entryEvicted(EntryEvent<String, Object> event) {
        //Don't care about this.
    }

    protected Configuration getGroupConfiguration(String groupName) throws ServiceException {
        try {
            Configuration[] configurations = configAdmin.listConfigurations("(" + GroupConfigurationImpl.GROUP_NAME_PROPERTY + "=" + groupName + ")");
            if ((configurations == null) || configurations.length == 0) {
                return null;
            }
            return configurations[0];
        } catch (Exception ex) {
            throw new ServiceException("Error occurred while attempting to retrieve the local configuration for group + " + groupName, ex);
        }
    }
}
