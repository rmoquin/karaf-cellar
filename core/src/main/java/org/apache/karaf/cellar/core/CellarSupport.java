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
    private static final transient Logger LOGGER = LoggerFactory.getLogger(CellarSupport.class);
    private SwitchConfiguration switchConfig;

    /**
     * Get a set of resources in the Cellar cluster groups configuration.
     *
     * @param listType the comma separated list of resources.
     * @param group the group name.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, String group, String category, EventType type) {
        Set<String> result = null;
        if (group != null) {
            if (switchConfig != null) {
                String parent = (String) switchConfig.getProperty(group + Configurations.SEPARATOR + Configurations.PARENT);
                if (parent != null) {
                    result = getListEntries(listType, parent, category, type);
                }

                String propertyName = group + Configurations.SEPARATOR + category + Configurations.SEPARATOR + listType + Configurations.SEPARATOR + type.name().toLowerCase();
                String propertyValue = (String) switchConfig.getProperty(propertyName);
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
     * @param clusters the cluster groups names.
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
     * Get a set of resources in the Cellar cluster groups configuration.
     *
     * @param listType a comma separated string of resources.
     * @param group the cluster.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, Group group, String category, EventType type) {
        Set<String> result = null;
        if (group != null) {
            String groupName = group.getName();
            Set<String> items = getListEntries(listType, groupName, category, type);
            if (items != null && !items.isEmpty()) {
                if (result == null) {
                    result = new HashSet<String>();
                }
                result.addAll(items);
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
    public Boolean isAllowed(Group group, String category, String event, EventType type) {
        Boolean result = true;
        Set<String> whiteList = getListEntries(Configurations.WHITELIST, group, category, type);
        Set<String> blackList = getListEntries(Configurations.BLACKLIST, group, category, type);

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
     * @return the switchConfig
     */
    public SwitchConfiguration getSwitchConfig() {
        return switchConfig;
    }

    /**
     * @param switchConfig the switchConfig to set
     */
    public void setSwitchConfig(SwitchConfiguration switchConfig) {
        this.switchConfig = switchConfig;
    }
}
