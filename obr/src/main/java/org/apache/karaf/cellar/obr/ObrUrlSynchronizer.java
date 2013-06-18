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
package org.apache.karaf.cellar.obr;

import java.util.Collection;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;

/**
 * OBR URL Synchronizer.
 */
public class ObrUrlSynchronizer extends ObrSupport implements Synchronizer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlSynchronizer.class);

    @Override
    public void init() {
        super.init();
        Collection<CellarCluster> clusters = clusterManager.getLocalClusters();
        if (clusters != null && !clusters.isEmpty()) {
            for (CellarCluster cluster : clusters) {
                if (isSyncEnabled(cluster)) {
                    pull(cluster);
                    push(cluster);
                } else {
                    LOGGER.debug("CELLAR OBR: sync is disabled for group {}", cluster.getName());
                }
            }
        }
    }
    
    @Override
    public boolean synchronize(CellarCluster cluster) {
        this.pull(cluster);
        this.push(cluster);
        return true;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Pull the OBR URLs from a cluster to update the local state.
     *
     * @param cluster the cluster.
     */
    @Override
    public void pull(CellarCluster cluster) {
        if (cluster != null) {
            String clusterName = cluster.getName();
            Set<String> clusterUrls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
            if (clusterUrls != null && !clusterUrls.isEmpty()) {
                for (String url : clusterUrls) {
                    try {
                        LOGGER.debug("CELLAR OBR: adding repository URL {}", url);
                        obrService.addRepository(url);
                    } catch (Exception e) {
                        LOGGER.error("CELLAR OBR: failed to add repository URL {}", url, e);
                    }
                }
            }
        }
    }

    /**
     * Push the local OBR URLs to a cluster.
     *
     * @param cluster the cluster.
     */
    @Override
    public void push(CellarCluster cluster) {
        if (cluster != null) {
            String clusterName = cluster.getName();
            Set<String> clusterUrls = clusterManager.getMasterCluster().getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);

            Repository[] repositories = obrService.listRepositories();
            for (Repository repository : repositories) {
                if (isAllowed(clusterName, Constants.URLS_CONFIG_CATEGORY, repository.getURI().toString(), EventType.OUTBOUND)) {
                    clusterUrls.add(repository.getURI().toString());
                    // update OBR bundles in the cluster group
                    Set<ObrBundleInfo> clusterBundles = clusterManager.getMasterCluster().getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
                    Resource[] resources = repository.getResources();
                    for (Resource resource : resources) {
                        ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                        clusterBundles.add(info);
                        // TODO fire event to the other nodes ?
                    }
                } else {
                    LOGGER.warn("CELLAR OBR: URL {} is blocked outbound for cluster group {}", repository.getURI().toString(), clusterName);
                }
            }
        }
    }

    @Override
    public Boolean isSyncEnabled(CellarCluster cluster) {
        String clusterName = cluster.getName();

        String propertyKey = clusterName + Configurations.SEPARATOR + Constants.URLS_CONFIG_CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
        String propertyValue = (String) this.synchronizationConfiguration.getProperty(propertyKey);
        return Boolean.parseBoolean(propertyValue);
    }
}
