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
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;

/**
 * The BundleEventHandler is responsible to process received cluster event for bundles.
 */
public class BundleEventHandler extends CommandHandler<ClusterBundleEvent, BundleEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;
    private CellarSupport cellarSupport;
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
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroup.getName());
            Set<String> bundleWhitelist = groupConfig.getInboundBundleWhitelist();
            Set<String> bundleBlacklist = groupConfig.getInboundBundleBlacklist();

            Set<String> featuresWhitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> featuresBlacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(this.location, bundleWhitelist, bundleBlacklist)) {
                // check the features first
                List<Feature> matchingFeatures = bundleSupport.retrieveFeature(this.location);
                for (Feature feature : matchingFeatures) {
                    if (!cellarSupport.isAllowed(feature.getName(), featuresWhitelist, featuresBlacklist)) {
                        LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED INBOUND for cluster group {}", location, this.symbolicName, this.sourceGroup.getName());
                        result.setSuccessful(false);
                        result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + this.location + " is contained in feature " + this.symbolicName + " marked BLOCKED INBOUND for cluster group " + this.sourceGroup.getName()));
                        return result;
                    }
                }
                if (this.type == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {} from {}", this.getId(), this.location);
                    bundleSupport.installBundleFromLocation(this.location);
                } else if (this.type == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}/{}", this.symbolicName, this.version);
                    bundleSupport.uninstallBundle(this.symbolicName, this.version);
                } else if (this.type == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{}", this.symbolicName, this.version);
                    bundleSupport.startBundle(this.symbolicName, this.version);
                } else if (this.type == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{}", this.symbolicName, this.version);
                    bundleSupport.stopBundle(this.symbolicName, this.version);
                } else if (this.type == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}/{}", this.symbolicName, this.version);
                    bundleSupport.updateBundle(this.symbolicName, this.version);
                }
            } else {
                LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND in cluster group {}", this.location, this.sourceGroup.getName());
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + this.location + " is marked BLOCKED INBOUND in cluster group " + this.sourceGroup.getName()));
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
     * Get the cluster bundle event handler switch.
     *
     * @return the cluster bundle event handler switch.
     */
    @Override
    public Switch getSwitch() {
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
