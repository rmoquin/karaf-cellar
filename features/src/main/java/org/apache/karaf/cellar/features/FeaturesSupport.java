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

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.karaf.cellar.core.CellarCluster;

/**
 * Generic features support.
 */
public class FeaturesSupport extends CellarSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSupport.class);

    protected FeaturesService featuresService;

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Check if a feature is already installed locally.
     *
     * @param name the feature name.
     * @param version the feature version.
     * @return true if the feature is already installed locally, false else.
     */
    public Boolean isFeatureInstalledLocally(String name, String version) {
        if (featuresService != null) {
            Feature[] localFeatures = featuresService.listInstalledFeatures();

            if (localFeatures != null && localFeatures.length > 0) {
                for (Feature localFeature : localFeatures) {
                    if (localFeature.getName().equals(name) && (localFeature.getVersion().equals(version) || version == null))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a features repository is already registered locally.
     *
     * @param uri the features repository URI.
     * @return true if the features repository is already registered locally, false else.
     */
    public Boolean isRepositoryRegisteredLocally(String uri) {
        Repository[] localRepositories = featuresService.listRepositories();
        for (Repository localRepository : localRepositories) {
            if (localRepository.getURI().toString().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Push a {@code Feature} and its status in a cluster.
     *
     * @param feature the feature to push in the cluster.
     * @param the cluster.
     */
    public void pushFeature(Feature feature, CellarCluster cluster) {
        if (feature != null) {
            String clusterName = cluster.getName();
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + clusterName);
LOGGER.warn("Pushing feature for cluster: " + clusterName);
            if (isAllowed(cluster.getName(), Constants.FEATURES_CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && clusterFeatures != null) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    Boolean installed = featuresService.isInstalled(feature);
                    clusterFeatures.put(info, installed);
LOGGER.warn("Feature for cluster: " + clusterName + " " + clusterFeatures);
                }
            } else LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster {}", feature.getName(), clusterName);
        } else LOGGER.warn("CELLAR FEATURES: feature is null");
    }

    /**
     * Push a {@code Feature} and its status in a cluster.
     * This version of the method force the bundle status, without looking the features service.
     *
     * @param feature the feature to push in the cluster.
     * @param cluster the cluster.
     * @param force true to force the bundle status (ignoring the features service), false else.
     */
    public void pushFeature(Feature feature, CellarCluster cluster, Boolean force) {
        if (feature != null) {
            String clusterName = cluster.getName();
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + clusterName);

            if (isAllowed(cluster.getName(), Constants.FEATURES_CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && clusterFeatures != null) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    clusterFeatures.put(info, force);
                }
            } else LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster {}", feature.getName());
        } else LOGGER.warn("CELLAR FEATURES: feature is null");
    }

    /**
     * Push a features {@code Repository} in a cluster.
     *
     * @param repository the features repository to push in the cluster.
     * @param cluster the cluster.
     */
    public void pushRepository(Repository repository, CellarCluster cluster) {
        String clusterName = cluster.getName();
        List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + clusterName);

        boolean found = false;
        for (String clusterRepository : clusterRepositories) {
            if (clusterRepository.equals(repository.getURI().toString())) {
                found = true;
                break;
            }
        }

        if (!found) {
            clusterRepositories.add(repository.getURI().toString());
        }
    }

    /**
     * Remove a features {@code Repository} from a cluster.
     *
     * @param repository the features repository to remove from the cluster.
     * @param cluster the cluster.
     */
    public void removeRepository(Repository repository, CellarCluster cluster) {
        String clusterName = cluster.getName();
        List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + clusterName);

        if (featuresService != null && clusterRepositories != null) {
            URI uri = repository.getURI();
            clusterRepositories.remove(uri.toString());
        }
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
