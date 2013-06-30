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

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.TcpIpConfig;
import java.io.FileNotFoundException;
import org.apache.karaf.cellar.core.discovery.Discovery;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.hazelcast.GenericCellarSerializer;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.osgi.framework.BundleContext;

/**
 * Hazelcast configuration manager.
 * It loads hazelcast.xml configuration file.
 */
public class HazelcastConfigurationManager {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastConfigurationManager.class);
    private BundleContext bundleContext;
    private BundleClassLoader bundleClassLoader;
    private String xmlConfig;
    private Set<String> discoveredMemberSet = new LinkedHashSet<String>();

    /**
     * Update configuration of a Hazelcast instance.
     *
     * @param properties the updated configuration properties.
     */
    public void update(Map properties) throws InterruptedException {
        if (properties != null) {
            if (properties.containsKey(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME)) {
                Set<String> newDiscoveredMemberSet = CellarUtils.createSetFromString((String) properties.get(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME));
                if (!CellarUtils.collectionEquals(discoveredMemberSet, newDiscoveredMemberSet)) {
                    LOGGER.info("Hazelcast discoveredMemberSet has been changed from {} to {}", discoveredMemberSet, newDiscoveredMemberSet);
                    discoveredMemberSet = newDiscoveredMemberSet;
                }
            }
        }
    }

    /**
     * Build a Hazelcast {@link com.hazelcast.config.Config}.
     *
     * @return the Hazelcast configuration.
     */
    public Config getHazelcastConfig(String clusterName) {
        Config cfg = null;
        try {
            cfg = new FileSystemXmlConfig(xmlConfig);
        } catch (FileNotFoundException ex) {
            LOGGER.error("The default hazelcast config couldn't be found, hazelcast will try to drop back to it's internal default.", ex);
            cfg = new Config();
        }
        if (clusterName != null) {
            cfg.getGroupConfig().setName(clusterName);
        }
        cfg.setClassLoader(bundleClassLoader);
        GlobalSerializerConfig globalConfig = new GlobalSerializerConfig();
        globalConfig.setClassName("java.lang.Object");
        globalConfig.setImplementation(new GenericCellarSerializer());
        cfg.getSerializationConfig().setGlobalSerializerConfig(globalConfig);
        if (discoveredMemberSet != null) {
            TcpIpConfig tcpIpConfig = cfg.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.getMembers().addAll(discoveredMemberSet);
        }
        return cfg;
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
     * @return the bundleClassLoader
     */
    public BundleClassLoader getBundleClassLoader() {
        return bundleClassLoader;
    }

    /**
     * @param bundleClassLoader the bundleClassLoader to set
     */
    public void setBundleClassLoader(BundleClassLoader bundleClassLoader) {
        this.bundleClassLoader = bundleClassLoader;
    }
}
