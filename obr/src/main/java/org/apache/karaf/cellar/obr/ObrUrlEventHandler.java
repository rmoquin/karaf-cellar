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
package org.apache.karaf.cellar.obr;

import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for cluster OBR URL event.
 */
public class ObrUrlEventHandler extends ObrSupport implements EventHandler<ClusterObrUrlEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.urls.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private CellarSupport cellarSupport;
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Handle a received cluster OBR URL event.
     *
     * @param event the received cluster OBR URL event.
     */
    @Override
    public void handle(ClusterObrUrlEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR OBR: switch is OFF", SWITCH_ID);
            return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR OBR: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }
        String url = event.getUrl();
        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(event.getSourceGroup().getName());
        Set<String> whitelist = groupConfig.getInboundConfigurationWhitelist();
        Set<String> blacklist = groupConfig.getInboundConfigurationBlacklist();
            if (cellarSupport.isAllowed(url, whitelist, blacklist) || event.getForce()) {
                LOGGER.debug("CELLAR OBR: received OBR URL {}", url);
                if (event.getType() == Constants.URL_ADD_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: adding repository URL {}", url);
                    obrService.addRepository(url);
                }
                if (event.getType() == Constants.URL_REMOVE_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: removing repository URL {}", url);
                    boolean removed = obrService.removeRepository(url);
                    if (!removed) {
                        LOGGER.warn("CELLAR OBR: repository URL {} has not been added to the OBR service", url);
                    }
                }
            } else {
                LOGGER.warn("CELLAR OBR: repository URL {} is marked BLOCKED INBOUND for cluster group {}", url, event.getSourceGroup().getName());
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to register repository URL {}", url, e);
        }
    }

    @Override
    public Class<ClusterObrUrlEvent> getType() {
        return ClusterObrUrlEvent.class;
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Boolean status = this.nodeConfiguration.getEnabledEventHandlers().contains(this.getClass().getName());
            if (status) {
                eventSwitch.turnOn();
            } else {
                eventSwitch.turnOff();
            }
        } catch (Exception e) {
            // ignore
        }
        return this.eventSwitch;
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
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }
}
