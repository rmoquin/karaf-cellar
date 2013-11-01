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

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.util.ArrayList;
import java.util.List;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Cellar bundle support.
 */
public class BundleSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleSupport.class);
    protected BundleContext bundleContext;
    protected FeaturesService featuresService;
    protected DistributedExecutionContext executionContext;

    /**
     * Locally install a bundle.
     *
     * @param location the bundle location.
     * @throws BundleException in case of installation failure.
     */
    public void installBundleFromLocation(String location) throws BundleException {
        getBundleContext().installBundle(location);
    }

    /**
     * Locally uninstall a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of un-installation failure.
     */
    public void uninstallBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.uninstall();
                }
            }
        }
    }

    /**
     * Locally start a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of start failure.
     */
    public void startBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.start();
                }
            }
        }
    }

    /**
     * Locally stop a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of stop failure.
     */
    public void stopBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.stop();
                }
            }
        }
    }

    /**
     * Locally update a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of update failure.
     */
    public void updateBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.update();
                }
            }
        }
    }

    /**
     * Get the list of features where the bundle is belonging.
     *
     * @param bundleLocation the bundle location.
     * @return the list of feature where the bundle is present.
     * @throws Exception in case of retrieval failure.
     */
    protected List<Feature> retrieveFeature(String bundleLocation) throws Exception {
        Feature[] features = featuresService.listFeatures();
        List<Feature> matchingFeatures = new ArrayList<Feature>();
        for (Feature feature : features) {
            List<BundleInfo> bundles = feature.getBundles();
            for (BundleInfo bundleInfo : bundles) {
                String location = bundleInfo.getLocation();
                if (location.equalsIgnoreCase(bundleLocation)) {
                    matchingFeatures.add(feature);
                    LOGGER.debug("CELLAR BUNDLE: found a feature {} containing bundle {}", feature.getName(), bundleLocation);
                }
            }
        }
        return matchingFeatures;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featureService) {
        this.featuresService = featureService;
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
