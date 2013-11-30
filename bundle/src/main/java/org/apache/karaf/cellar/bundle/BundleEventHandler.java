/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.bundle;

import java.text.MessageFormat;
import org.apache.karaf.features.Feature;
import org.osgi.framework.BundleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;

/**
 * The BundleEventHandler is responsible to process received cluster event for bundles.
 */
public class BundleEventHandler extends CommandHandler<ClusterBundleEvent, BundleEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private BundleSupport bundleSupport;

    /**
     * Handle received bundle cluster events.
     *
     * @param command the received bundle cluster command.
     * @return
     */
    @Override
    public BundleEventResponse execute(ClusterBundleEvent command) {
        BundleEventResponse result = new BundleEventResponse();
        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            result.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID)));
            LOGGER.debug("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            result.setSuccessful(false);
            return result;
        }

        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(command.getSourceGroup().getName());
            Set<String> bundleWhitelist = groupConfig.getInboundBundleWhitelist();
            Set<String> bundleBlacklist = groupConfig.getInboundBundleBlacklist();

            Set<String> featuresWhitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> featuresBlacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(command.getLocation(), bundleWhitelist, bundleBlacklist)) {
                // check the features first
                List<Feature> matchingFeatures = bundleSupport.retrieveFeature(command.getLocation());
                for (Feature feature : matchingFeatures) {
                    if (!cellarSupport.isAllowed(feature.getName(), featuresWhitelist, featuresBlacklist)) {
                        LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED INBOUND for cluster group {}", command.getLocation(), command.getSymbolicName(), command.getSourceGroup().getName());
                        result.setSuccessful(false);
                        result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + command.getLocation() + " is contained in feature " + command.getSymbolicName() + " marked BLOCKED INBOUND for cluster group " + command.getSourceGroup().getName()));
                        return result;
                    }
                }
                if (command.getType() == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {} from {}", command.getId(), command.getLocation());
                    bundleSupport.installBundleFromLocation(command.getLocation());
                } else if (command.getType() == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}/{}", command.getSymbolicName(), command.getVersion());
                    bundleSupport.uninstallBundle(command.getSymbolicName(), command.getVersion());
                } else if (command.getType() == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{}", command.getSymbolicName(), command.getVersion());
                    bundleSupport.startBundle(command.getSymbolicName(), command.getVersion());
                } else if (command.getType() == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{}", command.getSymbolicName(), command.getVersion());
                    bundleSupport.stopBundle(command.getSymbolicName(), command.getVersion());
                } else if (command.getType() == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}/{}", command.getSymbolicName(), command.getVersion());
                    bundleSupport.updateBundle(command.getSymbolicName(), command.getVersion());
                }
            } else {
                LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND in cluster group {}", command.getLocation(), command.getSourceGroup().getName());
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + command.getLocation() + " is marked BLOCKED INBOUND in cluster group " + command.getSourceGroup().getName()));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
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
        boolean status = nodeConfiguration.getEnabledEvents().contains(Configurations.HANDLER + "." + this.getType().getName());
        if (status) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
    }

    /**
     * Get the cluster event type.
     *
     * @return the cluster bundle event type.
     */
    @Override
    public Class<ClusterBundleEvent> getType() {
        return ClusterBundleEvent.class;
    }
}
