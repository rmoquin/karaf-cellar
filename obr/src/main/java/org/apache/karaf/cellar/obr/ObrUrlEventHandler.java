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

import java.text.MessageFormat;
import java.util.Set;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for cluster OBR URL event.
 */
public class ObrUrlEventHandler extends CommandHandler<ClusterObrUrlEvent, ClusterObrEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.urls.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private RepositoryAdmin obrService;

    public void init() {
    }

    public void destroy() {
    }

    /**
     * Handle a received cluster OBR URL event.
     *
     * @param event the received cluster OBR URL event.
     */
    @Override
    public ClusterObrEventResponse execute(ClusterObrUrlEvent event) {
        ClusterObrEventResponse result = new ClusterObrEventResponse(event.getId());
        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR OBR: {} switch is OFF", SWITCH_ID);
            result.setSuccessful(false);
            result.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR FEATURES: {0} switch is OFF, cluster event is not handled", SWITCH_ID)));
            return result;
        }

        final String sourceGroupName = event.getSourceGroup().getName();
        // check if the node is local
        if (!groupManager.isLocalGroup(sourceGroupName)) {
            result.setThrowable(new CommandExecutionException(MessageFormat.format("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName)));
            LOGGER.warn("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName);
            result.setSuccessful(false);
            return result;
        }

        String url = event.getUrl();
        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroupName);
            Set<String> whitelist = groupConfig.getInboundConfigurationWhitelist();
            Set<String> blacklist = groupConfig.getInboundConfigurationBlacklist();
            if (cellarSupport.isAllowed(url, whitelist, blacklist) || event.getForce()) {
                if (event.getType() == Constants.UrlEventTypes.URL_ADD_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: adding repository URL {}", url);
                    obrService.addRepository(url);
                }
                if (event.getType() == Constants.UrlEventTypes.URL_REMOVE_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: removing repository URL {}", url);
                    boolean removed = obrService.removeRepository(url);
                    if (!removed) {
                        LOGGER.warn("CELLAR OBR: repository URL {} has not been added to the OBR service", url);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to register repository URL {}", url, e);
            result.setThrowable(e);
            result.setSuccessful(false);
        }
        return result;
    }

    @Override
    public Class<ClusterObrUrlEvent> getType() {
        return ClusterObrUrlEvent.class;
    }

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        boolean status = nodeConfiguration.getEnabledEvents().contains(this.getType().getName());
        if (status) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
    }

    /**
     * @return the obrService
     */
    public RepositoryAdmin getObrService() {
        return obrService;
    }

    /**
     * @param obrService the obrService to set
     */
    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }
}
