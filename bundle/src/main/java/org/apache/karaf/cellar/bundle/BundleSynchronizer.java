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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;

/**
 * The BundleSynchronizer is called when Cellar starts or a node joins a cluster group.
 * The purpose is to synchronize bundles local state with the states in the cluster groups.
 */
public class BundleSynchronizer extends BundleSupport implements Synchronizer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleSynchronizer.class);
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;
    private CellarCluster masterCluster;
    private CellarSupport cellarSupport;

    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
<<<<<<< HEAD
        for (Group group : groups) {
            if (isSyncEnabled(group)) {
                pull(group);
                push(group);
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("CELLAR BUNDLE: sync is disabled for cluster group {}", group.getName());
                }
=======
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.debug("CELLAR BUNDLE: sync is disabled for cluster group {}", group.getName());
>>>>>>> remotes/apache/trunk
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Pull the bundles states from a cluster group.
     *
     * @param group the cluster group where to get the bundles states.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR BUNDLE: pulling bundles from cluster group {}", groupName);
            Map<String, BundleState> clusterBundles = masterCluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            for (Map.Entry<String, BundleState> entry : clusterBundles.entrySet()) {
                String id = entry.getKey();
                BundleState state = entry.getValue();

                String[] tokens = id.split("/");
                String symbolicName = tokens[0];
                String version = tokens[1];
                if (tokens != null && tokens.length == 2) {
                    if (state != null) {
                        String bundleLocation = state.getLocation();
                        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
                        Set<String> whitelist = groupConfig.getInboundBundleWhitelist();
                        Set<String> blacklist = groupConfig.getInboundBundleBlacklist();
                        if (cellarSupport.isAllowed(bundleLocation, whitelist, blacklist)) {
                            try {
                                if (state.getStatus() == BundleEvent.INSTALLED) {
                                    installBundleFromLocation(state.getLocation());
                                } else if (state.getStatus() == BundleEvent.STARTED) {
                                    installBundleFromLocation(state.getLocation());
                                    startBundle(symbolicName, version);
                                }
<<<<<<< HEAD
                            } catch (BundleException e) {
                                LOGGER.error("CELLAR BUNDLE: failed to pull bundle {}", id, e);
                            }
                        } else {
                            LOGGER.debug("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", bundleLocation, groupName);
=======
                            } else LOGGER.debug("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", bundleLocation, groupName);
>>>>>>> remotes/apache/trunk
                        }
                    }
                }
            }
        }
    }

    /**
     * Push local bundles states to a cluster group.
     *
     */
    @Override
    public void push(Group group) {

<<<<<<< HEAD
=======
        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR BUNDLE: cluster event producer is OFF");
            return;
        }

>>>>>>> remotes/apache/trunk
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR BUNDLE: pushing bundles to cluster group {}", groupName);
            Map<String, BundleState> clusterBundles = masterCluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            Bundle[] bundles = super.bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                String symbolicName = bundle.getSymbolicName();
                String version = bundle.getVersion().toString();
                String bundleLocation = bundle.getLocation();
                int status = bundle.getState();
                String id = symbolicName + "/" + version;

                // check if the pid is marked as local.
                GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(group.getName());
                Set<String> whitelist = groupConfig.getOutboundBundleWhitelist();
                Set<String> blacklist = groupConfig.getOutboundBundleBlacklist();
                if (cellarSupport.isAllowed(bundleLocation, whitelist, blacklist)) {
                    BundleState bundleState = new BundleState();
                    // get the bundle name or location.
                    String name = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                    // if there is no name, then default to symbolic name.
                    name = (name == null) ? bundle.getSymbolicName() : name;
                    // if there is no symbolic name, resort to location.
                    name = (name == null) ? bundle.getLocation() : name;
                    bundleState.setName(name);
                    bundleState.setName(bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME));
                    bundleState.setLocation(bundleLocation);

                    if (status == Bundle.ACTIVE) {
                        status = BundleEvent.STARTED;
                    }
                    if (status == Bundle.INSTALLED) {
                        status = BundleEvent.INSTALLED;
                    }
                    if (status == Bundle.RESOLVED) {
                        status = BundleEvent.RESOLVED;
                    }
                    if (status == Bundle.STARTING) {
                        status = BundleEvent.STARTING;
                    }
                    if (status == Bundle.UNINSTALLED) {
                        status = BundleEvent.UNINSTALLED;
                    }
                    if (status == Bundle.STOPPING) {
                        status = BundleEvent.STARTED;
                    }

<<<<<<< HEAD
                    bundleState.setStatus(status);

                    BundleState existingState = clusterBundles.get(id);

                    if (existingState == null ||
                            !existingState.getLocation().equals(bundleState.getLocation()) ||
                            existingState.getStatus() != bundleState.getStatus()) {
                        // update the distributed map
                        clusterBundles.put(id, bundleState);

                        // broadcast the event
                        BundleEventTask event = new BundleEventTask(symbolicName, version, bundleLocation, status);
                        event.setSourceGroup(group);
                        executionContext.executeAndWait(event, group.getNodesExcluding(this.groupManager.getNode()));
                    }

                } else {
                    LOGGER.debug("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, groupName);
=======
                    } else LOGGER.debug("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, groupName);
>>>>>>> remotes/apache/trunk
                }
            }
        }
    }

    /**
     * Check if the bundle sync flag is enabled for a cluster group.
     *
     * @return true if the sync flag is enabled, false else.
     */
    @Override
    public Boolean isSyncEnabled(Group group) {
        String groupName = group.getName();
        return this.groupManager.findGroupConfigurationByName(groupName).isSyncBundles();
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
     * @return the masterCluster
     */
    public CellarCluster getMasterCluster() {
        return masterCluster;
    }

    /**
     * @param masterCluster the masterCluster to set
     */
    public void setMasterCluster(CellarCluster masterCluster) {
        this.masterCluster = masterCluster;
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
