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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;

/**
 * Abstract cluster feature shell command.
 */
public abstract class FeatureCommandSupport extends CellarCommandSupport {
    protected static final transient Logger LOGGER = LoggerFactory.getLogger(FeatureCommandSupport.class);
    protected FeaturesService featuresService;
    protected CellarSupport cellarSupport = new CellarSupport();
    protected DistributedExecutionContext executionContext;
    
    /**
     * Force the features status for a specific group.
     * Why? Its required if no group member currently in the cluster.
     * If a member of the group joins later, it won't find the change, unless we force it.
     *
     * @param groupName the cluster group name.
     * @param feature the feature name.
     * @param version the feature version.
     * @param status the feature status (installed, uninstalled).
     * @return 
     */
    public Boolean updateFeatureStatus(String groupName, String feature, String version, Boolean status) {

        Boolean result = Boolean.FALSE;
        Group group = groupManager.findGroupByName(groupName);
        if (group == null || group.getNodes().isEmpty()) {

            FeatureInfo info = new FeatureInfo(feature, version);
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            // check the existing configuration
            if (version == null || (version.trim().length() < 1)) {
                for (FeatureInfo f : clusterFeatures.keySet()) {
                    if (f.getName().equals(feature)) {
                        version = f.getVersion();
                        info.setVersion(version);
                    }
                }
            }

            // check the features service
            try {
                for (Feature f : featuresService.listFeatures()) {
                    if (f.getName().equals(feature)) {
                        version = f.getVersion();
                        info.setVersion(version);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while browsing features", e);
            }

            if (info.getVersion() != null && (info.getVersion().trim().length() > 0)) {
                clusterFeatures.put(info, status);
                result = Boolean.TRUE;
            }
        }
        return result;
    }

    /**
     * Check if a feature is present in the cluster group.
     *
     * @param groupName the cluster group.
     * @param feature the feature name.
     * @param version the feature version.
     * @return true if the feature exists in the cluster group, false else.
     */
    public boolean featureExists(String groupName, String feature, String version) {
        Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

        if (clusterFeatures == null) {
            return false;
        }

        for (FeatureInfo distributedFeature : clusterFeatures.keySet()) {
            if (version == null) {
                if (distributedFeature.getName().equals(feature)) {
                    return true;
                }
            } else {
                if (distributedFeature.getName().equals(feature) && distributedFeature.getVersion().equals(version)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a cluster features event is allowed.
     *
     * @param name
     * @param whitelist
     * @param blacklist
     * @return true if the cluster features event is allowed, false else.
     */
    public boolean isAllowed(String name, Set<String> whitelist, Set<String> blacklist) {
        return cellarSupport.isAllowed(name, whitelist, blacklist);
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    /**
     * @return the executionContext
     */
    public DistributedExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @param executionContext the executionContext to set
     */
    public void setExecutionContext(DistributedExecutionContext executionContext) {
        this.executionContext = executionContext;
    }
}
