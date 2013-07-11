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
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.ManageGroupCommand;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;
import org.apache.karaf.cellar.hazelcast.serialization.CellarGenericSerializer;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.apache.karaf.cellar.hazelcast.serialization.CellarGroupSerializer;
import org.apache.karaf.cellar.hazelcast.serialization.CellarNodeSerializer;
import org.apache.karaf.cellar.hazelcast.serialization.ManageGroupCommandSerializer;

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
    public void update(Map properties) throws InterruptedException {
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

    /**
     * Build a Hazelcast {@link com.hazelcast.config.Config}.
     *
     * @return the Hazelcast configuration.
     */
    public Config createHazelcastConfig(String clusterName, String nodeName, BundleClassLoader hzClassLoader) throws FileNotFoundException {
        Config config = this.createHazelcastConfig(clusterName, nodeName);
        if (hzClassLoader != null) {
            config.setClassLoader(hzClassLoader);
        }
        return config;
    }

    /**
     * Build a Hazelcast {@link com.hazelcast.config.Config}.
     *
     * @return the Hazelcast configuration.
     */
    public Config createHazelcastConfig(String clusterName, String nodeName) throws FileNotFoundException {
        Config cfg = new FileSystemXmlConfig(xmlConfig);
        cfg.setInstanceName(nodeName);
        cfg.getGroupConfig().setName(clusterName);

        SerializerConfig sc = new SerializerConfig().setImplementation(new CellarGroupSerializer()).setTypeClass(Group.class);

        SerializerConfig node = new SerializerConfig().setImplementation(new CellarNodeSerializer()).setTypeClass(HazelcastNode.class);

        SerializerConfig manageGroupCommand = new SerializerConfig().setImplementation(new ManageGroupCommandSerializer()).setTypeClass(ManageGroupCommand.class);

        cfg.getSerializationConfig().addSerializerConfig(sc);
        cfg.getSerializationConfig().addSerializerConfig(node);
        cfg.getSerializationConfig().addSerializerConfig(manageGroupCommand);

        GlobalSerializerConfig globalConfig = new GlobalSerializerConfig();
        globalConfig.setClassName("java.lang.Object");
        globalConfig.setImplementation(new CellarGenericSerializer());
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
