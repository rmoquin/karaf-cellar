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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.FileNotFoundException;

/**
 * A factory for Hazelcast instance, integrating with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);
    private HazelcastConfigurationManager configManager;
    private BundleContext bundleContext;

    private HazelcastInstance instance;

    public void init() throws FileNotFoundException {
        this.instance = Hazelcast.newHazelcastInstance(configManager.createHazelcastConfig());
    }

    public void destroy() {
        if (instance != null) {
            Hazelcast.shutdownAll();
        }
    }

    public void update(Map properties) {
        if (configManager.isUpdated(properties)) {
            LOGGER.debug("CELLAR HAZELCAST: configuration update is true");
            Config config = instance.getConfig();
            TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            List<String> members = tcpIpConfig.getMembers();

            Set<String> discoveredMemberSet = configManager.getDiscoveredMemberSet();
            discoveredMemberSet.removeAll(members);

            if (!discoveredMemberSet.isEmpty()) {
                LOGGER.debug("CELLAR HAZELCAST: will add following members {}", discoveredMemberSet);
                for (String discoveredMember : discoveredMemberSet) {
                    tcpIpConfig.addMember(discoveredMember);
                }
                if (!tcpIpConfig.isEnabled()) {
                    LOGGER.debug("CELLAR HAZELCAST: tcpip mode needs to be enabled, will do now!");
                    tcpIpConfig.setEnabled(true);
                }
            }
        }
    }

    /**
     * Get the Hazelcast instance.
     *
     * @return the Hazelcast instance.
     */
    public HazelcastInstance getInstance() {
        return instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the configManager
     */
    public HazelcastConfigurationManager getConfigManager() {
        return configManager;
    }

    /**
     * @param configManager the configManager to set
     */
    public void setConfigManager(HazelcastConfigurationManager configManager) {
        this.configManager = configManager;
    }
}
