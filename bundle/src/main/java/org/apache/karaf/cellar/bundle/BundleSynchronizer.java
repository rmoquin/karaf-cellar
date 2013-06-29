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
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The BundleSynchronizer is called when Cellar starts or a node joins a cluster group.
 * The purpose is to synchronize bundles local state with the states in the cluster groups.
 */
public class BundleSynchronizer extends BundleSupport implements Synchronizer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleSynchronizer.class);

    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.warn("CELLAR BUNDLE: sync is disabled for cluster group {}", group.getName());
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Pull the bundles states from a cluster group.
     *
     * @param cluster the cluster group where to get the bundles states.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR BUNDLE: pulling bundles from cluster group {}", groupName);
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            for (Map.Entry<String, BundleState> entry : clusterBundles.entrySet()) {
                String id = entry.getKey();
                BundleState state = entry.getValue();

                String[] tokens = id.split("/");
                String symbolicName = tokens[0];
                String version = tokens[1];
                if (tokens != null && tokens.length == 2) {
                    if (state != null) {
                        String bundleLocation = state.getLocation();
                            if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
                            try {
                                if (state.getStatus() == BundleEvent.INSTALLED) {
                                    installBundleFromLocation(state.getLocation());
                                } else if (state.getStatus() == BundleEvent.STARTED) {
                                    installBundleFromLocation(state.getLocation());
                                    startBundle(symbolicName, version);
                                }
                            } catch (BundleException e) {
                                LOGGER.error("CELLAR BUNDLE: failed to pull bundle {}", id, e);
                            }
                            } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", bundleLocation, groupName);
                        }
                    }
                }
            }
        }
    }

    /**
     * Push local bundles states to a cluster group.
     *
     * @param cluster the cluster where to update the bundles states.
     */
    @Override
    public void push(Group group) {

        // check if the producer is ON
        if (!cluster.emitsEvents()) {
            LOGGER.warn("CELLAR BUNDLE: cluster event producer is OFF");
            return;
        }

        if (group != null) {
            String groupName = group.getName();
        LOGGER.debug("CELLAR BUNDLE: pushing bundles to cluster group {}", groupName);
        Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

        Bundle[] bundles;

        bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            String symbolicName = bundle.getSymbolicName();
            String version = bundle.getVersion().toString();
            String bundleLocation = bundle.getLocation();
            int status = bundle.getState();
            String id = symbolicName + "/" + version;

            // check if the pid is marked as local.
                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {

                BundleState bundleState = new BundleState();
                // get the bundle name or location.
                String name = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                // if there is no name, then default to symbolic name.
                name = (name == null) ? bundle.getSymbolicName() : name;
                // if there is no symbolic name, resort to location.
                name = (name == null) ? bundle.getLocation() : name;
                bundleState.setName(name);
                bundleState.setLocation(bundleLocation);

                        if (status == Bundle.ACTIVE)
                    status = BundleEvent.STARTED;
                        if (status == Bundle.INSTALLED)
                    status = BundleEvent.INSTALLED;
                        if (status == Bundle.RESOLVED)
                    status = BundleEvent.RESOLVED;
                        if (status == Bundle.STARTING)
                    status = BundleEvent.STARTING;
                        if (status == Bundle.UNINSTALLED)
                    status = BundleEvent.UNINSTALLED;
                        if (status == Bundle.STOPPING)
                    status = BundleEvent.STARTED;

                bundleState.setStatus(status);

                BundleState existingState = clusterBundles.get(id);

                if (existingState == null ||
                        !existingState.getLocation().equals(bundleState.getLocation()) ||
                        existingState.getStatus() != bundleState.getStatus()) {
                    // update the distributed map
                    clusterBundles.put(id, bundleState);

                    // broadcast the event
                    ClusterBundleEvent event = new ClusterBundleEvent(symbolicName, version, bundleLocation, status);
                            event.setSourceGroup(group);
                    cluster.produce(event);
                        }

                    } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, groupName);
                }
            }
        }
    }

    /**
     * Check if the bundle sync flag is enabled for a cluster group.
     *
     * @param cluster the cluster group to check.
     * @return true if the sync flag is enabled, false else.
     */
    @Override
    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
            String propertyValue = (String) super.synchronizationConfiguration.getProperty(propertyKey);
            result = Boolean.parseBoolean(propertyValue);
        } catch (Exception e) {
            LOGGER.error("CELLAR BUNDLE: error while checking if sync is enabled", e);
        }
        return result;
    }
}
