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

import java.text.MessageFormat;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;

/**
 * Handler for cluster features event.
 */
public class FeaturesEventHandler extends CommandHandler<ClusterFeaturesEvent, FeatureEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.features.handler";

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
     * Handle a received cluster features event.
     *
     * @param event the received cluster feature event.
     * @return
     */
    @Override
    public FeatureEventResponse execute(ClusterFeaturesEvent event) {

        FeatureEventResponse result = new FeatureEventResponse();
        if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR FEATURES: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            result.setSuccessful(false);
            result.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR FEATURES: {0} switch is OFF, cluster event is not handled", SWITCH_ID)));
            return result;
        }

        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(event.getSourceGroup().getName());
            Set<String> whitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> blacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(event.getName(), whitelist, blacklist)) {
                boolean isInstalled = featuresSupport.isFeatureInstalledLocally(event.getName(), event.getVersion());
                if (FeatureEvent.EventType.FeatureInstalled.equals(event.getType()) && !isInstalled) {
                    boolean noClean = event.getNoClean();
                    boolean noRefresh = event.getNoRefresh();
                    EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);
                    if (noClean) {
                        options.add(FeaturesService.Option.NoCleanIfFailure);
                    }
                    if (noRefresh) {
                        options.add(FeaturesService.Option.NoAutoRefreshBundles);
                    }
                    if (event.getVersion() != null) {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", event.getName(), event.getVersion());
                        featuresService.installFeature(event.getName(), event.getVersion(), options);
                    } else {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}", event.getName());
                        featuresService.installFeature(event.getName(), options);
                    }
                } else if (FeatureEvent.EventType.FeatureUninstalled.equals(event.getType()) && isInstalled) {
                    if (event.getVersion() != null) {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", event.getName(), event.getVersion());
                        featuresService.uninstallFeature(event.getName(), event.getVersion());
                    } else {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}", event.getName());
                        featuresService.uninstallFeature(event.getName());
                    }
                }
                result.setSuccessful(true);
            } else {
                LOGGER.error("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group", event.getName(), event.getSourceGroup().getName());
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group " + event.getName()));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR FEATURES: failed to handle configuration task event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    /**
     * Get the event type that this handler is able to handle.
     *
     * @return the cluster features event type.
     */
    @Override
    public Class<ClusterFeaturesEvent> getType() {
        return ClusterFeaturesEvent.class;
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
