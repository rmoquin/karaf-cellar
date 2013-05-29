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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import java.util.Map;

import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.hazelcast.internal.HazelcastClassLoader;
import org.osgi.framework.BundleContext;

/**
 * Factory for Hazelcast instance, including integration with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceImpl implements HazelcastService {
    private BundleContext bundleContext;
    private HazelcastConfigurationManager configurationManager = new HazelcastConfigurationManager();
    private HazelcastInstance instance;

    public void init() {
        Config config = configurationManager.getHazelcastConfig();
        if (bundleContext != null) {
            config.setClassLoader(this.getClass().getClassLoader());
        }
        instance = Hazelcast.newHazelcastInstance(config);
    }

    public void shutdown() {
        if (instance != null) {
            instance.getLifecycleService().shutdown();
        }
    }

    public void update(Map properties) throws InterruptedException {
        configurationManager.isUpdated(properties);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the instance
     */
    @Override
    public HazelcastInstance getInstance() {
        return instance;
    }

    /**
     * @param instance the instance to set
     */
    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }
}
