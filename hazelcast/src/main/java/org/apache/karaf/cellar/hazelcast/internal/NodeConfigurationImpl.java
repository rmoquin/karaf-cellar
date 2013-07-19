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
import java.util.Set;
import org.apache.karaf.cellar.core.NodeConfiguration;

/**
 *
 * @author rmoquin
 */
public class NodeConfigurationImpl implements NodeConfiguration {
    private Map<String, Object> properties;

    public void update(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * @return the groupNames
     */
    @Override
    public Set<String> getGroupNames() {
        return (Set<String>) this.properties.get(NodeConfiguration.GROUPS_PROPERTY);
    }

    /**
     * @param groupNames the groupNames to set
     */
    public void setGroupNames(Set<String> groupNames) {
        this.properties.put(NodeConfiguration.GROUPS_PROPERTY, groupNames);
    }

    /**
     * @return the producer
     */
    @Override
    public boolean isProducer() {
        return (Boolean) this.properties.get(NodeConfiguration.PRODUCER_PROPERTY);
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer(boolean producer) {
        this.properties.put(NodeConfiguration.PRODUCER_PROPERTY, producer);
    }

    /**
     * @return the consumer
     */
    @Override
    public boolean isConsumer() {
        return (Boolean) this.properties.get(NodeConfiguration.CONSUMER_PROPERTY);
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(boolean consumer) {
        this.properties.put(NodeConfiguration.CONSUMER_PROPERTY, consumer);
    }

    /**
     * @return the enabledEventHandlers
     */
    @Override
    public Set<String> getEnabledEventHandlers() {
        return (Set<String>) this.properties.get(NodeConfiguration.ENABLED_EVENTS_PROPERTY);
    }

    /**
     * @param enabledEventHandlers the enabledEventHandlers to set
     */
    public void setEnabledEventHandlers(Set<String> enabledEventHandlers) {
        this.properties.put(NodeConfiguration.ENABLED_EVENTS_PROPERTY, enabledEventHandlers);
    }

}
