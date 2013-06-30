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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupManager;

/**
 * LocalBundleListener is listening for local bundles changes.
 * When a local bundle change occurs, this listener updates the cluster and broadcasts a cluster bundle event.
 */
public class LocalBundleListener extends BundleSupport implements SynchronousBundleListener {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalBundleListener.class);
    private EventProducer eventProducer;
    private GroupManager groupManager;
    private CellarCluster masterCluster;
    private CellarSupport cellarSupport;

    /**
     * Callback method called when a local bundle status change.
     *
     * @param event the local bundle event.
     */
    @Override
    public void bundleChanged(BundleEvent event) {

        if (event.getBundle().getBundleId() == 0 && (event.getType() == BundleEvent.STOPPING || event.getType() == BundleEvent.STOPPED)) {
            LOGGER.debug("CELLAR BUNDLE: Karaf shutdown detected, removing Cellar LocalBundleListener");
            bundleContext.removeBundleListener(this);
            return;
        }

        if (event.getBundle().getBundleId() == 0) {
            return;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR BUNDLE: cluster event producer is OFF");
            return;
        }

        if (event != null && event.getBundle() != null) {
            Set<Group> groups = null;
            try {
                groups = groupManager.listLocalGroups();
            } catch (Exception ex) {
                LOGGER.warn("Failed to list local groups. Is Cellar uninstalling ?", ex);
            }

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {
                    // get the bundle name or location.
                    String name = (String) event.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                    // if there is no name, then default to symbolic name.
                    name = (name == null) ? event.getBundle().getSymbolicName() : name;
                    // if there is no symbolic name, resort to location.
                    name = (name == null) ? event.getBundle().getLocation() : name;
                    String symbolicName = event.getBundle().getSymbolicName();
                    String version = event.getBundle().getVersion().toString();
                    String bundleLocation = event.getBundle().getLocation();
                    int type = event.getType();

                    if (cellarSupport.isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {
                        try {
                            // update bundles in the cluster group
                            Map<String, BundleState> clusterBundles = masterCluster.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + group.getName());
                            if (type == BundleEvent.UNINSTALLED) {
                                clusterBundles.remove(symbolicName + "/" + version);
                            } else {
                                BundleState state = clusterBundles.get(symbolicName + "/" + version);
                                if (state == null) {
                                    state = new BundleState();
                                }
                                state.setName(name);
                                state.setStatus(type);
                                state.setLocation(bundleLocation);
                                clusterBundles.put(symbolicName + "/" + version, state);
                            }

                            // check the features first
                            List<Feature> matchingFeatures = retrieveFeature(bundleLocation);
                            for (Feature feature : matchingFeatures) {
            					if (!cellarSupport.isAllowed(group, "features", feature.getName(), EventType.OUTBOUND)) {
            						LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, feature.getName(), group.getName());
                                    return;
                                }
                            }

                            // broadcast the cluster event
                            ClusterBundleEvent clusterBundleEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, type);
                            clusterBundleEvent.setSourceGroup(group);
                            eventProducer.produce(clusterBundleEvent);
                        } catch (Exception e) {
                            LOGGER.error("CELLAR BUNDLE: failed to create bundle event", e);
                        }

                    } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, group.getName());
                }
            }
        }
    }

    public void init() {
        getBundleContext().addBundleListener(this);
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
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

}
