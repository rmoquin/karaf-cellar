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
package org.apache.karaf.cellar.event;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;

/**
 * Handler for cluster event.
 */
public class ClusterEventHandler extends EventSupport implements EventHandler<ClusterEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ClusterEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private NodeConfiguration switchConfig;
    private GroupManager groupManager;
    private CellarSupport cellarSupport;

    @Override
    public void handle(ClusterEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR EVENT: {} is OFF, cluster event not handled", SWITCH_ID);
            return;
        }

        if (groupManager == null) {
            // in rare cases for example right after installation this happens!
            LOGGER.error("CELLAR EVENT: retrieved event {} while groupManager is not available yet!", event);
            return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR EVENT: node is not part of the event cluster group");
            return;
        }

        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(event.getSourceGroup().getName());
            Set<String> whitelist = groupConfig.getInboundConfigurationWhitelist();
            Set<String> blacklist = groupConfig.getInboundConfigurationBlacklist();
            if (cellarSupport.isAllowed(event.getTopicName(), whitelist, blacklist)) {
                Map<String, Serializable> properties = event.getProperties();
                properties.put(Constants.EVENT_PROCESSED_KEY, Constants.EVENT_PROCESSED_VALUE);
                properties.put(Constants.EVENT_SOURCE_GROUP_KEY, event.getSourceGroup());
                properties.put(Constants.EVENT_SOURCE_NODE_KEY, event.getSourceNode());
                postEvent(event.getTopicName(), properties);
            } else {
                LOGGER.warn("CELLAR EVENT: event {} is marked BLOCKED INBOUND for cluster group {}", event.getTopicName(), event.getSourceGroup().getName());
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR EVENT: failed to handle event", e);
        }
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
        try {
            boolean status = switchConfig.getEnabledEventHandlers().contains(this.getClass().getName());
            if (status) {
                eventSwitch.turnOn();
            } else {
                eventSwitch.turnOff();
            }
        } catch (Exception e) {
            // ignore
        }
        return eventSwitch;
    }

    /**
     * Get the event type handled by this handler.
     *
     * @return the cluster event type.
     */
    @Override
    public Class<ClusterEvent> getType() {
        return ClusterEvent.class;
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
     * @return the switchConfig
     */
    public NodeConfiguration getSwitchConfig() {
        return switchConfig;
    }

    /**
     * @param switchConfig the switchConfig to set
     */
    public void setSwitchConfig(NodeConfiguration switchConfig) {
        this.switchConfig = switchConfig;
    }
}
