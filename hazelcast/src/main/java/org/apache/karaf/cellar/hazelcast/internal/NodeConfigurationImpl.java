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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
@Component(name = "org.apache.karaf.cellar.core.NodeConfiguration", configurationFactory = false, label = "Node Configuration", ds = false,
        metatype = true, description = "The list of groups that the current node belongs to and control of which configurations will be synced to or from it.")
public class NodeConfigurationImpl implements NodeConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(NodeConfigurationImpl.class);
    @Property(label = "Groups", unbounded = PropertyUnbounded.VECTOR, description = "The names of the groups this node belongs to.")
    public static final String GROUPS_PROPERTY = "groups";
    @Property(label = "Produces Events", boolValue = true, description = "Whether or not this node will send synchronization events to other nodes in it's group.")
    public static final String PRODUCER_PROPERTY = "producer";
    @Property(label = "Consumes Events", boolValue = true, description = "Whether or not this node will consume synchronization events sent from other nodes in it's group.")
    public static final String CONSUMER_PROPERTY = "consumer";
    @Property(label = "Enabled Events", unbounded = PropertyUnbounded.VECTOR, options = {
        @PropertyOption(name = "org.apache.karaf.cellar.bundle.BundleEventHandler", value = "Bundle Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.config.ConfigurationEventHandler", value = "Configuration Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.features.FeaturesEventHandler", value = "Feature Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.dosgi.RemoteServiceCallHandler", value = "DOSGi Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.event.ClusterEventHandler", value = "Cluster Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.obr.ObrBundleEventHandler", value = "OBR Bundle Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.obr.ObrUrlEventHandler", value = "OBR Events")
    })
    public static final String ENABLE_EVENTS_PROPERTY = "enabledEvents";
    private Map<String, Object> properties = new HashMap<String, Object>();

    public void updated(Map<String, Object> properties) {
        LOGGER.warn("Node Configuration update properties was called!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11111111111" + properties);
        if (this.properties != null) {
            this.properties.putAll(properties);
        }
    }

    /**
     * @return the groupNames
     */
    @Override
    public Set<String> getGroups() {
        return (Set<String>) this.properties.get(GROUPS_PROPERTY);
    }

    /**
     * @param groups the groupNames to set
     */
    public void setGroups(Set<String> groups) {
        this.properties.put(GROUPS_PROPERTY, groups);
    }

    /**
     * @return the producer
     */
    @Override
    public boolean isProducer() {
        return (Boolean) this.properties.get(PRODUCER_PROPERTY);
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer(boolean producer) {
        this.properties.put(PRODUCER_PROPERTY, producer);
    }

    /**
     * @return the consumer
     */
    @Override
    public boolean isConsumer() {
        return (Boolean) this.properties.get(CONSUMER_PROPERTY);
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(boolean consumer) {
        this.properties.put(CONSUMER_PROPERTY, consumer);
    }

    /**
     * @return the enabledEventHandlers
     */
    @Override
    public Set<String> getEnabledEvents() {
        return (Set<String>) this.properties.get(ENABLE_EVENTS_PROPERTY);
    }

    /**
     * @param enabledEvents the enabledEventHandlers to set
     */
    public void setEnabledEvents(Set<String> enabledEvents) {
        this.properties.put(ENABLE_EVENTS_PROPERTY, enabledEvents);
    }

    /**
     * @return the properties
     */
    @Override
    public Dictionary<String, Object> getProperties() {
        Hashtable ht = new Hashtable();
        ht.putAll(properties);
        return ht;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
