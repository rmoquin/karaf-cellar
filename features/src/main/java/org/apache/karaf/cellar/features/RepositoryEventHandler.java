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
package org.apache.karaf.cellar.features;

import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.MessageFormat;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;
import org.apache.karaf.features.FeaturesService;

/**
 * Handler for cluster features repository event.
 */
public class RepositoryEventHandler extends CommandHandler<ClusterRepositoryEvent, RespositoryEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RepositoryEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.repository.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private final FeaturesSupport featuresSupport = new FeaturesSupport();
    private FeaturesService featuresService;

    public void init() {
        featuresSupport.setClusterManager(clusterManager);
        featuresSupport.setGroupManager(groupManager);
        featuresSupport.setFeaturesService(featuresService);
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Handle cluster features repository event.
     *
     * @param event
     * @return
     */
    @Override
    public RespositoryEventResponse execute(ClusterRepositoryEvent event) {

        RespositoryEventResponse result = new RespositoryEventResponse(event.getId());
        // check if the handler is ON
        if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR FEATURES: {} switch is OFF, cluster event is not handled", SWITCH_ID);
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

        String uri = event.getId();
        RepositoryEvent.EventType type = event.getType();
        try {
            // TODO check if isAllowed
            if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                if (!featuresSupport.isRepositoryRegisteredLocally(uri)) {
                    LOGGER.debug("CELLAR FEATURES: adding repository URI {}", uri);
                    featuresService.addRepository(new URI(uri), event.getInstall());
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is already registered locally");
                }
            } else {
                if (featuresSupport.isRepositoryRegisteredLocally(uri)) {
                    LOGGER.debug("CELLAR FEATURES: removing repository URI {}", uri);
                    featuresService.removeRepository(new URI(uri), event.getUninstall());
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is not registered locally");
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR FEATURES: failed to add/remove repository URL {}", uri, e);
            result.setThrowable(e);
            result.setSuccessful(false);
        }
        return result;
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

    @Override
    public Class<ClusterRepositoryEvent> getType() {
        return ClusterRepositoryEvent.class;
    }

    /**
     * @return the featuresService
     */
    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    /**
     * @param featuresService the featuresService to set
     */
    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
}
