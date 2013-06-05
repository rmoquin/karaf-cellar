/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.karaf.cellar.hazelcast.internal;

import java.util.Map;
import org.apache.karaf.cellar.core.SynchronizationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class HazelcastSynchronizationConfiguration implements SynchronizationConfiguration{
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastSynchronizationConfiguration.class);
    private Map<String, Object> properties;
    
    public void updated(Map<String, Object> config) {
        LOGGER.info("Sync config was updated: " + config);
        if (config == null) {
            return;
        }
        this.properties = config;
    }

    /**
     * @return the properties
     */
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
