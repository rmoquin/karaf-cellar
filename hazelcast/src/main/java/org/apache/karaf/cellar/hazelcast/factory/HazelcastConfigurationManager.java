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
import com.hazelcast.config.TcpIpConfig;
import java.io.FileNotFoundException;
import org.apache.karaf.cellar.core.discovery.Discovery;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;

/**
 * Hazelcast configuration manager. It loads hazelcast.xml configuration file.
 */
public class HazelcastConfigurationManager {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastConfigurationManager.class);
    private Set<String> discoveredMemberSet = new LinkedHashSet<String>();
    private String xmlConfig;
    private BundleClassLoader hzClassLoader;
    private String clusterName;
    private String nodeName;

    public Config createHazelcastConfig() throws FileNotFoundException {
        Config cfg = new FileSystemXmlConfig(xmlConfig);
        cfg.setInstanceName(nodeName);
        cfg.getGroupConfig().setName(clusterName);
        if (hzClassLoader != null) {
            cfg.setClassLoader(hzClassLoader);
        }
        /*     GlobalSerializerConfig globalConfig = new GlobalSerializerConfig();
         globalConfig.setClassName("java.lang.Object");
         globalConfig.setImplementation(new GenericCellarSerializer<Object>(10, Object.class));
         cfg.getSerializationConfig().setGlobalSerializerConfig(globalConfig);*/
//        SerializerConfig serializerConfig1 = new SerializerConfig();
//        serializerConfig1.setImplementation(new GenericCellarSerializer<Node>(11, Node.class));
//        serializerConfig1.setTypeClass(Node.class);
//        SerializerConfig serializerConfig2 = new SerializerConfig();
//        serializerConfig2.setImplementation(new GenericCellarSerializer<Group>(12, Group.class));
//        serializerConfig2.setTypeClass(Group.class);
//
//        cfg.getSerializationConfig().addSerializerConfig(serializerConfig1);
//        cfg.getSerializationConfig().addSerializerConfig(serializerConfig2);
        if (discoveredMemberSet != null) {
            TcpIpConfig tcpIpConfig = cfg.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.getMembers().addAll(discoveredMemberSet);
        }
        return cfg;
    }

    /**
     * Update configuration of a Hazelcast instance.
     *
     * @param properties the updated configuration properties.
     * @return
     */
    public boolean isUpdated(Map properties) {
        if (properties != null) {
            if (properties.containsKey(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME)) {
                Set<String> newDiscoveredMemberSet = CellarUtils.createSetFromString((String) properties.get(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME));
                if (!CellarUtils.collectionEquals(discoveredMemberSet, newDiscoveredMemberSet)) {
                    LOGGER.debug("CELLAR HAZELCAST: Hazelcast discoveredMemberSet has been changed from {} to {}", discoveredMemberSet, newDiscoveredMemberSet);
                    discoveredMemberSet = newDiscoveredMemberSet;
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> getDiscoveredMemberSet() {
        return discoveredMemberSet;
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
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName the nodeName to set
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
