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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for cluster OBR URL event.
 */
public class ObrUrlEventHandler extends ObrSupport implements EventHandler<ClusterObrUrlEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.urls.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

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

        String url = event.getUrl();
        try {
            if (isAllowed(event.getSourceCluster().getName(), Constants.URLS_CONFIG_CATEGORY, url, EventType.INBOUND) || event.getForce()) {
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
            Boolean status = Boolean.parseBoolean((String) super.synchronizationConfiguration.getProperty(Configurations.HANDLER + "." + this.getClass().getName()));
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
}
