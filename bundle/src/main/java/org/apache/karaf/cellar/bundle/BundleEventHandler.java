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

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.features.Feature;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;

/**
 * The BundleEventHandler is responsible to process received cluster event for bundles.
 */
public class BundleEventHandler extends BundleSupport implements EventHandler<ClusterBundleEvent> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;
    private CellarSupport cellarSupport;

    /**
     * Handle received bundle cluster events.
     *
     * @param event the received bundle cluster event.
     */
    @Override
    public void handle(ClusterBundleEvent event) {

        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.info("CELLAR BUNDLE: node is not part of the event cluster group");
            return;
        }
        try {
            // check if the pid is marked as local.
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(event.getSourceGroup().getName());
            Set<String> bundleWhitelist = groupConfig.getInboundBundleWhitelist();
            Set<String> bundleBlacklist = groupConfig.getInboundBundleBlacklist();

            Set<String> featuresWhitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> featuresBlacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(event.getLocation(), bundleWhitelist, bundleBlacklist)) {
                // check the features first
                List<Feature> matchingFeatures = retrieveFeature(event.getLocation());
                for (Feature feature : matchingFeatures) {
                    if (!cellarSupport.isAllowed(feature.getName(), featuresWhitelist, featuresBlacklist)) {
                        LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED INBOUND for cluster group {}", event.getLocation(), feature.getName(), event.getSourceGroup().getName());
                        return;
                    }
                }
                if (event.getType() == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {} from {}", event.getId(), event.getLocation());
                    installBundleFromLocation(event.getLocation());
                } else if (event.getType() == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    uninstallBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    startBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    stopBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    updateBundle(event.getSymbolicName(), event.getVersion());
                }
            } else {
                LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND in cluster group {}", event.getLocation(), event.getSourceGroup().getName());
            }
        } catch (BundleException e) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
        } catch (Exception e) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
        }
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
        boolean status = this.nodeConfiguration.getEnabledEventHandlers().contains(this.getClass().getName());
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
