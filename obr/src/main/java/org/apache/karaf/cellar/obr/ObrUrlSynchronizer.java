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

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;

/**
 * OBR URL Synchronizer.
 */
public class ObrUrlSynchronizer implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlSynchronizer.class);
    private final CellarSupport cellarSupport = new CellarSupport();
    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private RepositoryAdmin obrService;

    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else {
                    LOGGER.debug("CELLAR OBR: sync is disabled for group {}", group.getName());
                }
            }
        }
    }

    public void destroy() {
    }

    /**
     * Pull the OBR URLs from a cluster group to update the local state.
     *
     * @param group the cluster group.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> clusterUrls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
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
     * Push the local OBR URLs to a cluster group.
     *
     * @param group the cluster group.
     */
    @Override
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> clusterUrls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);

            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(group.getName());
            Set<String> whitelist = groupConfig.getOutboundOBRUrlsWhitelist();
            Set<String> blacklist = groupConfig.getOutboundOBRUrlsBlacklist();

            Repository[] repositories = obrService.listRepositories();
            for (Repository repository : repositories) {

                if (cellarSupport.isAllowed(repository.getURI().toString(), whitelist, blacklist)) {
                    clusterUrls.add(repository.getURI().toString());
                    // update OBR bundles in the cluster group
                    Set<ObrBundleInfo> clusterBundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
                    Resource[] resources = repository.getResources();
                    for (Resource resource : resources) {
                        ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                        clusterBundles.add(info);
                        // TODO fire event to the other nodes ?
                    }
                } else {
                    LOGGER.debug("CELLAR OBR: URL {} is marked BLOCKED OUTBOUND for cluster group {}", repository.getURI().toString(), groupName);
                }
            }
        }
    }

    @Override
    public Boolean isSyncEnabled(Group group) {
        String groupName = group.getName();
        return this.groupManager.findGroupConfigurationByName(groupName).isSyncOBRUrls();
    }

    /**
     * @return the clusterManager
     */
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    /**
     * @param clusterManager the clusterManager to set
     */
    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
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
     * @return the obrService
     */
    public RepositoryAdmin getObrService() {
        return obrService;
    }

    /**
     * @param obrService the obrService to set
     */
    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }
}
