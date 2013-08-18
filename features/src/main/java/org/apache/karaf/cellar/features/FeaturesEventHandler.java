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

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import org.apache.karaf.cellar.core.GroupConfiguration;

/**
 * Handler for cluster features event.
 */
public class FeaturesEventHandler extends FeaturesSupport implements EventHandler<ClusterFeaturesEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.features.handler";
    private static final String SWITCH_HANDLER_NAME = FeaturesEventHandler.class.getName();
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
     * Handle a received cluster features event.
     *
     * @param event the received cluster feature event.
     */
    @Override
    public void handle(ClusterFeaturesEvent event) {

        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR FEATURES: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR FEATURES: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }
        String name = event.getName();
        String version = event.getVersion();
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(event.getSourceGroup().getName());
        Set<String> whitelist = groupConfig.getInboundFeatureWhitelist();
        Set<String> blacklist = groupConfig.getInboundFeatureBlacklist();
        if (cellarSupport.isAllowed(name, whitelist, blacklist) || event.getForce()) {
            FeatureEvent.EventType type = event.getType();
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
            } catch (Exception e) {
                LOGGER.error("CELLAR FEATURES: failed to handle cluster feature event", e);
            }
        } else {
            LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group {}", name, event.getSourceGroup().getName());
        }
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
        try {
            Boolean status = super.nodeConfiguration.getEnabledEvents().contains(SWITCH_HANDLER_NAME);
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
}
