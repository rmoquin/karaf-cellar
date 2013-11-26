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

import com.sun.corba.se.impl.activation.CommandHandler;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

/**
 * Handler for cluster features event.
 */
public class FeaturesEventHandler implements CommandHandler<ClusterRepositoryEvent, ClusterRepositoryResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.features.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private FeaturesSupport featuresSupport = new FeaturesSupport();

    /**
     * Handle a received cluster features event.
     *
     * @param event the received cluster feature event.
     */
    public void handle(ClusterFeaturesEvent event) {

        if (executionContezt.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR FEATURES: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        FeatureEventResponse result = new FeatureEventResponse();
        try {

            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroup.getName());
            Set<String> whitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> blacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(name, whitelist, blacklist)) {
                boolean isInstalled = false;
                Boolean isInstalled = isFeatureInstalledLocally(name, version);
                try {
                    if (FeatureEvent.EventType.FeatureInstalled.equals(type) && !isInstalled) {
                        boolean noClean = event.getNoClean();
                        boolean noRefresh = event.getNoRefresh();
                        EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);
                        if (noClean) {
                            options.add(FeaturesService.Option.NoCleanIfFailure);
                        }
                        if (noRefresh) {
                            options.add(FeaturesService.Option.NoAutoRefreshBundles);
                        }
                        if (version != null) {
                            LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", name, version);
                            featuresService.installFeature(name, version, options);
                        } else {
                            LOGGER.debug("CELLAR FEATURES: installing feature {}", name);
                            featuresService.installFeature(name, options);
                        }
                    } else if (FeatureEvent.EventType.FeatureUninstalled.equals(type) && isInstalled) {
                        if (version != null) {
                            LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", name, version);
                            featuresService.uninstallFeature(name, version);
                        } else {
                            LOGGER.debug("CELLAR FEATURES: un-installing feature {}", name);
                            featuresService.uninstallFeature(name);
                        }
                    }
                    result.setSuccessful(true);
                }else {
                LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group", name, sourceGroup.getName());
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group " + name));
            }
            }catch (Exception ex) {
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
        public Class<ClusterFeaturesEvent> getType


            () {
        return ClusterFeaturesEvent.class;
        }

        /**
         * Get the handler switch.
         *
         * @return the handler switch.
         */
        @Override
        public Switch getSwitch


            () {
        // load the switch status from the config
        try {
                Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
                if (configuration != null) {
                    Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                    if (status) {
                        eventSwitch.turnOn();
                    } else {
                        eventSwitch.turnOff();
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            return eventSwitch;
        }

    }
