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
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Factory for Hazelcast instance, including integration with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceImpl implements HazelcastService {
    private BundleContext bundleContext;
    private HazelcastConfigurationManager configurationManager = new HazelcastConfigurationManager();
    private HazelcastInstance instance;

    public void init() {
        //Just because I'm tired of dealing with the cp stuff right now, temporarily use the TTCL workaround here.
        Config config = configurationManager.getHazelcastConfig();
        Bundle b = FrameworkUtil.getBundle(com.hazelcast.config.Config.class);
        ClassLoader priorClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			Thread.currentThread().setContextClassLoader(new BundleClassLoader(b));
			instance = Hazelcast.newHazelcastInstance(config);
		} finally {
			Thread.currentThread().setContextClassLoader(priorClassLoader);
		}
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
