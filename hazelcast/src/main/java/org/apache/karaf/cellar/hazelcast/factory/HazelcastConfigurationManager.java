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
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.TcpIpConfig;
import java.io.FileNotFoundException;
import org.apache.karaf.cellar.core.discovery.Discovery;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.tasks.ManageGroupTask;
import org.apache.karaf.cellar.hazelcast.serialization.GenericCellarSerializer;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.apache.karaf.cellar.hazelcast.serialization.ManageGroupTaskSerializer;

/**
 * Hazelcast configuration manager.
 * It loads hazelcast.xml configuration file.
 */
public class HazelcastConfigurationManager {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastConfigurationManager.class);
    private Set<String> discoveredMemberSet = new LinkedHashSet<String>();
    private String xmlConfig;
    private BundleClassLoader hzClassLoader;

    /**
     * Update configuration of a Hazelcast instance.
     *
     * @param properties the updated configuration properties.
     */
    public void update(Map<String, Object> properties) {
        if (properties != null) {
            if (properties.containsKey(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME)) {
                Set<String> newDiscoveredMemberSet = CellarUtils.createSetFromString((String) properties.get(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME));
                if (!CellarUtils.collectionEquals(discoveredMemberSet, newDiscoveredMemberSet)) {
                    LOGGER.info("Hazelcast discoveredMemberSet has been changed from {0} to {1}", discoveredMemberSet, newDiscoveredMemberSet);
                    discoveredMemberSet = newDiscoveredMemberSet;
                }
            }
        }
    }

    public Config createHazelcastConfig(String clusterName, String nodeName) throws FileNotFoundException {
        Config cfg = new FileSystemXmlConfig(xmlConfig);
        cfg.setInstanceName(nodeName);
        cfg.getGroupConfig().setName(clusterName);
        if (hzClassLoader != null) {
            cfg.setClassLoader(hzClassLoader);
        }
        SerializerConfig serializerConfig = new SerializerConfig();
        serializerConfig.setImplementation(new ManageGroupTaskSerializer());
        serializerConfig.setTypeClass(ManageGroupTask.class);
        cfg.getSerializationConfig().addSerializerConfig(serializerConfig);

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
