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
package org.apache.karaf.cellar.core;

import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cellar generic support. This class provides a set of util methods used by other classes.
 */
public class CellarSupport {
    protected static final transient Logger LOGGER = LoggerFactory.getLogger(CellarSupport.class);
    protected SynchronizationConfiguration synchronizationConfiguration;

    /**
     * Get a set of resources in the Cellar cluster groups configuration.
     *
     * @param listType the comma separated list of resources.
     * @param cluster the cluster name.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, String cluster, String category, EventType type) {
        Set<String> result = null;
        if (cluster != null) {
            if (synchronizationConfiguration != null) {
                String parent = (String) synchronizationConfiguration.getProperty(cluster + Configurations.SEPARATOR + Configurations.PARENT);
                if (parent != null) {
                    result = getListEntries(listType, parent, category, type);
                }

                String propertyName = cluster + Configurations.SEPARATOR + category + Configurations.SEPARATOR + listType + Configurations.SEPARATOR + type.name().toLowerCase();
                String propertyValue = (String) synchronizationConfiguration.getProperty(propertyName);
                if (propertyValue != null) {
                    propertyValue = propertyValue.replaceAll("\n", "");
                    String[] itemList = propertyValue.split(Configurations.DELIMETER);

                    if (itemList != null && itemList.length > 0) {
                        if (result == null) {
                            result = new HashSet<String>();
                        }
                        for (String item : itemList) {
                            if (item != null) {
                                result.add(item.trim());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get the resources in the Cellar cluster groups configuration.
     *
     * @param listType the comma separated string of resources.
     * @param groups the cluster groups names.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, Collection<String> groups, String category, EventType type) {
        Set<String> result = null;
        if (groups != null && !groups.isEmpty()) {
            for (String group : groups) {
                Set<String> items = getListEntries(listType, group, category, type);
                if (items != null && !items.isEmpty()) {
                    if (result == null) {
                        result = new HashSet<String>();
                    }
                    result.addAll(items);
                }
            }
        }
        return result;
    }

    /**
     * Check if a resource is allowed for a type of cluster event.
     *
     * @param clusterName the cluster name.
     * @param category the resource category name.
     * @param event the resource name.
     * @param type the event type (inbound, outbound).
     */
    public Boolean isAllowed(String clusterName, String category, String event, EventType type) {
        Boolean result = true;
        Set<String> whiteList = getListEntries(Configurations.WHITELIST, clusterName, category, type);
        Set<String> blackList = getListEntries(Configurations.BLACKLIST, clusterName, category, type);

        // if no white listed items we assume all are accepted.
        if (whiteList != null && !whiteList.isEmpty()) {
            result = false;
            for (String whiteListItem : whiteList) {
                if (wildCardMatch(event, whiteListItem)) {
                    result = true;
                }
            }
        }

        // if any blackList item matched, then false is returned.
        if (blackList != null && !blackList.isEmpty()) {
            for (String blackListItem : blackList) {
                if (wildCardMatch(event, blackListItem)) {
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Check if a string match a regex.
     *
     * @param item the string to check.
     * @param pattern the regex pattern.
     * @return true if the item string matches the pattern, false else.
     */
    protected boolean wildCardMatch(String item, String pattern) {
        // update the pattern to have a valid regex pattern
        pattern = pattern.replace("*", ".*");
        // use the regex
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(item);
        return m.matches();
    }

    /**
     * @return the synchronizationConfiguration
     */
    public SynchronizationConfiguration getSynchronizationConfiguration() {
        return synchronizationConfiguration;
    }

    /**
     * @param synchronizationConfiguration the synchronizationConfiguration to set
     */
    public void setSynchronizationConfiguration(SynchronizationConfiguration synchronizationConfiguration) {
        this.synchronizationConfiguration = synchronizationConfiguration;
    }
}
