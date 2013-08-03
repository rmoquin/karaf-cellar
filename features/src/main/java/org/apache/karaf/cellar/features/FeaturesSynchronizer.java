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
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.GroupConfiguration;

/**
 * Features synchronizer.
 */
public class FeaturesSynchronizer extends FeaturesSupport implements Synchronizer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    @Override
    public void init() {
        super.init();
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (group.getNodes().size() > 1) {
                    if (isSyncEnabled(group)) {
                        pull(group);
                        push(group);
                    } else {
                        LOGGER.warn("CELLAR FEATURES: sync is disabled for cluster group {}", group.getName());
                    }
                } else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("CELLAR FEATURES: Group only has 1 member, synchronization will be skipped: {}", group.getName());
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Pull the features repositories and features states from a cluster, and update the local states.
     *
     * @param cluster the cluster group.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR FEATURES: pulling features repositories and features from cluster group {}", groupName);
            List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            clusterManager.getList(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            // get the features repositories URLs from the cluster group
            if (clusterRepositories != null && !clusterRepositories.isEmpty()) {
                for (String url : clusterRepositories) {
                    try {
                        if (!isRepositoryRegisteredLocally(url)) {
                            LOGGER.debug("CELLAR FEATURES: adding new features repository {}", url);
                            featuresService.addRepository(new URI(url));
                        }
                    } catch (MalformedURLException e) {
                        LOGGER.error("CELLAR FEATURES: failed to add features repository URL {} (malformed)", url, e);
                    } catch (Exception e) {
                        LOGGER.error("CELLAR FEATURES: failed to add features repository URL {}", url, e);
                    }
                }
            }

            // get the features from the cluster
            if (clusterFeatures != null && !clusterFeatures.isEmpty()) {
                for (FeatureInfo info : clusterFeatures.keySet()) {
                    String name = info.getName();
                    // check if feature is blocked
                    GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
                    List<String> whitelist = groupConfig.getOutboundFeatureWhitelist();
                    List<String> blacklist = groupConfig.getOutboundFeatureBlacklist();
                    if (cellarSupport.isAllowed(name, whitelist, blacklist)) {
                        Boolean remotelyInstalled = clusterFeatures.get(info);
                        Boolean locallyInstalled = isFeatureInstalledLocally(info.getName(), info.getVersion());

                        // prevent NPE
                        if (remotelyInstalled == null) {
                            remotelyInstalled = false;
                        }
                        if (locallyInstalled == null) {
                            locallyInstalled = false;
                        }

                        // if feature has to be installed locally
                        if (remotelyInstalled && !locallyInstalled) {
                            try {
                                LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", info.getName(), info.getVersion());
                                featuresService.installFeature(info.getName(), info.getVersion());
                            } catch (Exception e) {
                                LOGGER.warn("CELLAR FEATURES: failed to install feature {}/{} ", new Object[] { info.getName(), info.getVersion() }, e);
                            }
                            // if feature has to be uninstalled locally
                        } else if (!remotelyInstalled && locallyInstalled) {
                            try {
                                LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", info.getName(), info.getVersion());
                                featuresService.uninstallFeature(info.getName(), info.getVersion());
                            } catch (Exception e) {
                                LOGGER.warn("CELLAR FEATURES: failed to uninstall feature {}/{} ", new Object[] { info.getName(), info.getVersion() }, e);
                            }
                        }
                    } else {
                        LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group {}", name, groupName);
                    }
                }
            }
        }
    }

    /**
     * Push features repositories and features local states to a cluster group.
     *
     * @param group the cluster group.
     */
    @Override
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.info("CELLAR FEATURES: pushing features repositories and features in cluster group {}", groupName);
            clusterManager.getList(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            Repository[] repositoryList = new Repository[0];
            Feature[] featuresList = new Feature[0];

            try {
                repositoryList = featuresService.listRepositories();
                featuresList = featuresService.listFeatures();
            } catch (Exception e) {
                LOGGER.warn("CELLAR FEATURES: unable to list features", e);
            }

            // push features repositories to the cluster group
            if (repositoryList != null && repositoryList.length > 0) {
                for (Repository repository : repositoryList) {
                    pushRepository(repository, group);
                    LOGGER.debug("CELLAR FEATURES: pushing repository {} in cluster group {}", repository.getName(), group.getName());
                }
            }

            // push features to the cluster group
            if (featuresList != null && featuresList.length > 0) {
                for (Feature feature : featuresList) {
                    pushFeature(feature, group);
                    LOGGER.debug("CELLAR FEATURES: pushing feature {} in cluster group {}", feature.getName(), group.getName());
                }
            }
        }
    }

    /**
     * Check if the sync flag is enabled for a cluster group.
     *
     * @param cluster the cluster group.
     * @return true if the sync flag is enabled, false else.
     */
    @Override
    public Boolean isSyncEnabled(Group group) {
        String groupName = group.getName();
        return this.groupManager.findGroupConfigurationByName(groupName).isSyncFeatures();
    }
}