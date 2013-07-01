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
package org.apache.karaf.cellar.hazelcast.factory;

import java.util.Map;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.osgi.framework.BundleContext;

/**
 * Factory for Hazelcast instance, including integration with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceFactory {
    private BundleContext bundleContext;
    private BundleClassLoader hzClassLoader;
    private HazelcastConfigurationManager configurationManager = new HazelcastConfigurationManager();
    private HazelcastInstance instance;
    private String clusterName;
    private String xmlConfig;

    public void init() {
        System.setProperty("hazelcast.config", xmlConfig);
        this.instance = Hazelcast.newHazelcastInstance(configurationManager.getHazelcastConfig(clusterName, hzClassLoader));
    }

    public void destroy() {
        instance.getLifecycleService().shutdown();
    }

    public void update(Map properties) throws InterruptedException {
        configurationManager.update(properties);
    }

    /**
     * Return the local Hazelcast instance.
     *
     * @return the Hazelcast instance.
     */
    public HazelcastInstance getInstance() throws InterruptedException {
        return instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the hzClassLoader
     */
    public BundleClassLoader getHzClassLoader() {
        return hzClassLoader;
    }

    /**
     * @param hzClassLoader the hzClassLoader to set
     */
    public void setHzClassLoader(BundleClassLoader hzClassLoader) {
        this.hzClassLoader = hzClassLoader;
    }

    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    
        /**
     * @return the xmlConfig
     */
    public String getXmlConfig() {
        return xmlConfig;
    }

    /**
     * @param xmlConfig the xmlConfig to set
     */
    public void setXmlConfig(String xmlConfig) {
        this.xmlConfig = xmlConfig;
    }
}
