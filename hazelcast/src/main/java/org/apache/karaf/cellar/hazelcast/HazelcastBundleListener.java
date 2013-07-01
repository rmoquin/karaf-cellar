/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.hazelcast;

import java.net.URL;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast bundle listener.
 */
public class HazelcastBundleListener implements SynchronousBundleListener {
    private static Logger LOGGER = LoggerFactory.getLogger(HazelcastBundleListener.class);
    private BundleContext bundleContext;
    private Map<String, Vector<URL>> loadedResources = new ConcurrentHashMap<String, Vector<URL>>();

    public void init() {
        bundleContext.addBundleListener(this);
        scanExistingBundles();
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
        this.bundleContext = null;
        emptyResources();
        loadedResources.clear();
    }

    public void scanExistingBundles() {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            loadFromBundle(bundle);
        }
    }

//    @Override
    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTING:
            case BundleEvent.STARTED:
                loadFromBundle(event.getBundle());
                break;
            case BundleEvent.STOPPING:
            case BundleEvent.STOPPED:
                unloadFromBundle(event.getBundle());
                break;
            case BundleEvent.RESOLVED:
            case BundleEvent.UNINSTALLED:
        }
    }

    public void loadFromBundle(Bundle bundle) {
        //Bit of a dirty hack.
        if (bundle != null) {
            Dictionary dictionary = bundle.getHeaders();
            String bundleName = (String) dictionary.get(org.osgi.framework.Constants.BUNDLE_NAME);
            if ("hazelcast".equals(bundleName)) {
                Enumeration<URL> urls = bundle.findEntries("META-INF/services/", "*", true);
                if (urls == null) {
                    return;
                }
                if (urls.hasMoreElements()) {
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        String urlString = url.toString();
                        int i = urlString.lastIndexOf("/");
                        urlString = urlString.substring(i + 1);
                        Vector<URL> resources = loadedResources.get(urlString);
                        if (resources == null) {
                            resources = new Vector<URL>();
                            loadedResources.put(urlString, resources);
                        }
                        resources.add(url);
                    }
                }
            }
        }
    }

    public void unloadFromBundle(Bundle bundle) {
//Bit of a dirty hack.
        if (bundle != null) {
            Dictionary dictionary = bundle.getHeaders();
            String bundleName = (String) dictionary.get(org.osgi.framework.Constants.BUNDLE_NAME);
            if ("hazelcast".equals(bundleName)) {
                emptyResources();
            }
        }
    }

    private void emptyResources() {
        for (Map.Entry<String, Vector<URL>> entry : loadedResources.entrySet()) {
            Vector<URL> vector = entry.getValue();
            vector.clear();
        }
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the loadedResources
     */
    public Map<String, Vector<URL>> getResources() {
        return loadedResources;
    }
}
