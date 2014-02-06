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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.features.FeaturesListener;

/**
 * Local features listener.
 */
public class LocalFeaturesListener extends FeaturesSupport implements FeaturesListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalFeaturesListener.class);

    /**
     * This method is called when a local feature has changed.
     *
     * @param event the local feature event.
     */
    @Override
    public void featureEvent(FeatureEvent event) {

        // check if the producer is ON
        if (executionContext.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR FEATURES: cluster event producer is OFF");
            return;
        }
        if (event != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {
                    Feature feature = event.getFeature();
                    String featureName = feature.getName();
                    String featureVersion = feature.getVersion();
                    GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(group.getName());
                    Set<String> whitelist = groupConfig.getOutboundFeatureWhitelist();
                    Set<String> blacklist = groupConfig.getOutboundFeatureBlacklist();
                    if (this.isAllowed(featureName, whitelist, blacklist)) {
                        FeatureEvent.EventType type = event.getType();
                        boolean installedInGroup = featureExists(group.getName(), featureName, featureVersion);
                        boolean needsBroadcast = false;
                        // update the features in the cluster group
                        if (FeatureEvent.EventType.FeatureInstalled.equals(event.getType())) {
                            pushFeature(feature, group, true);
                            needsBroadcast = !installedInGroup;
                        } else {
                            pushFeature(feature, group, false);
                            needsBroadcast = installedInGroup;
                        }

                        if (needsBroadcast) {
                            // broadcast the event
                            ClusterFeaturesEvent featureEvent = new ClusterFeaturesEvent(featureName, featureVersion, type);
                            featureEvent.setSourceGroup(group);
                            executionContext.execute(featureEvent, group.getNodesExcluding(groupManager.getNode()));
                        }
                    } else {
                        LOGGER.debug("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster group {}", featureName, group.getName());
                    }
                }
            }
        }
    }

    /**
     * This method is called when a local features repository has changed.
     *
     * @param event
     */
    @Override
    public void repositoryEvent(RepositoryEvent event
    ) {

        //TODO Re-enable this functionaity,
        if (executionContext.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR FEATURES: cluster event producer is OFF");
            return;
        }
        if (event != null && event.getRepository() != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {
                    ClusterRepositoryEvent clusterRepositoryEvent = new ClusterRepositoryEvent(event.getRepository().getURI().toString(), event.getType());
                    clusterRepositoryEvent.setSourceGroup(group);
                    RepositoryEvent.EventType type = event.getType();

                    // update the features repositories in the cluster group
                    if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                        pushRepository(event.getRepository(), group);
                        // update the features in the cluster group

                        Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group.getName());
                        try {
                            for (Feature feature : event.getRepository().getFeatures()) {
                                // check the feature in the distributed map
                                FeatureInfo featureInfo = null;
                                for (FeatureInfo clusterFeature : clusterFeatures.keySet()) {
                                    if (clusterFeature.getName().equals(feature.getName()) && clusterFeature.getVersion().equals(feature.getVersion())) {
                                        featureInfo = clusterFeature;
                                        break;
                                    }
                                }
                                if (featureInfo == null) {
                                    featureInfo = new FeatureInfo(feature.getName(), feature.getVersion());
                                    clusterFeatures.put(featureInfo, false);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("CELLAR FEATURES: failed to update the cluster group", e);
                        }
                    } else {
                        removeRepository(event.getRepository(), group);
                        // update the features in the cluster group
                        Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group.getName());
                        try {
                            for (Feature feature : event.getRepository().getFeatures()) {
                                FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                                clusterFeatures.remove(info);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("CELLAR FEATURES: failed to update the cluster group", e);
                        }
                    }
                    // broadcast the cluster event
                    executionContext.executeAndWait(clusterRepositoryEvent, group.getNodesExcluding(groupManager.getNode()));
                }
            }
        }
    }
}
