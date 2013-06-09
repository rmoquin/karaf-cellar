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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import org.apache.karaf.cellar.core.SynchronizationConfiguration;

/**
 * Handler for cluster event.
 */
public class ClusterEventHandler extends EventSupport implements EventHandler<ClusterEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ClusterEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private SynchronizationConfiguration synchronizationConfig;

    @Override
    public void handle(ClusterEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR EVENT: {} is OFF, cluster event not handled", SWITCH_ID);
            return;
        }

        if (clusterManager == null) {
            // in rare cases for example right after installation this happens!
            LOGGER.error("CELLAR EVENT: retrieved event {} while groupManager is not available yet!", event);
            return;
        }

        try {
            if (isAllowed(event.getSourceCluster().getName(), Constants.CATEGORY, event.getTopicName(), EventType.INBOUND)) {
                Map<String, Serializable> properties = event.getProperties();
                properties.put(Constants.EVENT_PROCESSED_KEY, Constants.EVENT_PROCESSED_VALUE);
                properties.put(Constants.EVENT_SOURCE_CLUSTER_KEY, event.getSourceCluster().getName());
                properties.put(Constants.EVENT_SOURCE_NODE_KEY, event.getSourceNode());
                postEvent(event.getTopicName(), properties);
            } else {
                LOGGER.warn("CELLAR EVENT: event {} is marked BLOCKED INBOUND for cluster group {}", event.getTopicName(), event.getSourceCluster().getName());
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
            Boolean status = Boolean.parseBoolean((String) synchronizationConfig.getProperty(Configurations.HANDLER + "." + this.getClass().getName()));
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
     * @return the synchronizationConfig
     */
    public SynchronizationConfiguration getSynchronizationConfig() {
        return synchronizationConfig;
    }

    /**
     * @param synchronizationConfig the synchronizationConfig to set
     */
    public void setSynchronizationConfig(SynchronizationConfiguration synchronizationConfig) {
        this.synchronizationConfig = synchronizationConfig;
    }
}
