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
import org.apache.karaf.cellar.core.command.DistributedTask;

/**
 * The BundleEventTask is responsible to process received cluster event for bundles.
 */
public class BundleEventTask extends DistributedTask<BundleEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventTask.class);
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;
    private CellarSupport cellarSupport;
    private BundleSupport bundleSupport;

    private String id;
    private String symbolicName;
    private String version;
    private String location;
    private int type;

    public BundleEventTask() {
    }

    public BundleEventTask(String symbolicName, String version, String location, int type) {
        this.id = symbolicName + "/" + version;
        this.symbolicName = symbolicName;
        this.version = version;
        this.location = location;
        this.type = type;
    }

    /**
     * Handle received bundle cluster events.
     *
     * @return
     */
    @Override
    public BundleEventResponse execute() {
        BundleEventResponse result = new BundleEventResponse();
        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(this.sourceGroup.getName());
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

    public String getId() {
        this.id = symbolicName + "/" + version;
        return this.id;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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
