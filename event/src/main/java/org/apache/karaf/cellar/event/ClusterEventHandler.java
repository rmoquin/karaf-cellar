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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;

/**
 * Handler for cluster event.
 */
public class ClusterEventHandler extends CommandHandler<ClusterEvent, ClusterEventResult> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ClusterEventHandler.class);
    private final EventSupport eventSupport = new EventSupport();
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public ClusterEventResult execute(ClusterEvent event) {
        ClusterEventResult clusterEventResult = new ClusterEventResult();
        clusterEventResult.setId(event.getId());
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR EVENT: {} is OFF, cluster event not handled", SWITCH_ID);
            clusterEventResult.setSuccessful(false);
            clusterEventResult.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR EVENT: {} is OFF, cluster event not handled", SWITCH_ID)));
            return clusterEventResult;
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
                clusterEventResult.setProperties(properties);
                clusterEventResult.setTopicName(event.getTopicName());
            } else {
                LOGGER.warn("CELLAR EVENT: event {} is marked BLOCKED INBOUND for cluster group {}", event.getTopicName(), event.getSourceGroup().getName());
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR EVENT: failed to handle event", e);
        }
        return clusterEventResult;
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
            boolean status = nodeConfiguration.getEnabledEvents().contains(this.getClass().getName());
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
}
