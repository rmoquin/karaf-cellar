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
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;

/**
 * The BundleEventHandler is responsible to process received cluster event for bundles.
 */
public class BundleEventHandler extends CommandHandler<ClusterBundleEvent, BundleEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private final BundleSupport bundleSupport = new BundleSupport();
    private BundleContext bundleContext;
    private FeaturesService featuresService;

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
            result.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR BUNDLE: {0} switch is OFF, cluster event is not handled", SWITCH_ID)));
            LOGGER.debug("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            result.setSuccessful(false);
            return result;
        }

        String sourceGroupName = command.getSourceGroup().getName();
        // check if the node is local
        if (!groupManager.isLocalGroup(sourceGroupName)) {
            result.setThrowable(new CommandExecutionException(MessageFormat.format("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName)));
            LOGGER.warn("Node is not part of thiscluster group {}, commend will be ignored.", sourceGroupName);
            result.setSuccessful(false);
            return result;
        }
        String location = command.getLocation();
        String symbolicName = command.getSymbolicName();

        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroupName);
            Set<String> bundleWhitelist = groupConfig.getInboundBundleWhitelist();
            Set<String> bundleBlacklist = groupConfig.getInboundBundleBlacklist();

            if (bundleSupport.isAllowed(location, bundleWhitelist, bundleBlacklist)) {
                // check the features first
                Set<String> featuresWhitelist = groupConfig.getInboundFeatureWhitelist();
                Set<String> featuresBlacklist = groupConfig.getInboundFeatureBlacklist();
                List<Feature> matchingFeatures = bundleSupport.retrieveFeature(location);
                for (Feature feature : matchingFeatures) {
                    if (!bundleSupport.isAllowed(feature.getName(), featuresWhitelist, featuresBlacklist)) {
                        LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED INBOUND for cluster group {}", location, symbolicName, sourceGroupName);
                        result.setSuccessful(false);
                        result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + location + " is contained in feature " + symbolicName + " marked BLOCKED INBOUND for cluster group " + sourceGroupName));
                        return result;
                    }
                }
                final int type = command.getType();
                final String version = command.getVersion();
                if (type == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {}", command);
                    bundleSupport.installBundleFromLocation(location);
                } else if (type == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}", command);
                    bundleSupport.uninstallBundle(symbolicName, version);
                } else if (type == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}", command);
                    bundleSupport.startBundle(symbolicName, version);
                } else if (type == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}", command);
                    bundleSupport.stopBundle(symbolicName, version);
                } else if (type == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}", command);
                    bundleSupport.updateBundle(symbolicName, version);
                }
            } else {
                LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND in cluster group {}", location, sourceGroupName);
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR BUNDLE: bundle " + location + " is marked BLOCKED INBOUND in cluster group " + sourceGroupName));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    public void init() {
        bundleSupport.setBundleContext(bundleContext);
        bundleSupport.setFeaturesService(featuresService);
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
        boolean status = nodeConfiguration.getEnabledEvents().contains(this.getType().getName());
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

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featureService) {
        this.featuresService = featureService;
    }
}
