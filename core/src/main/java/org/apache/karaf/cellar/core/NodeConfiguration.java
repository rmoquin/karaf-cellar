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
package org.apache.karaf.cellar.core;

import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;

/**
 *
 * @author rmoquin
 */
@Component(label = "Node Configuration", ds = false, metatype = true, createPid = true, description = "The list of groups that the current node belongs to and control of which configurations will be synced to or from it.")
public interface NodeConfiguration {
    @Property(label = "Groups", unbounded = PropertyUnbounded.ARRAY, description = "The names of the groups this node belongs to.")
    static final String GROUPS_PROPERTY = "groups";

    @Property(label = "Produces Events", unbounded = PropertyUnbounded.ARRAY, description = "Whether or not this node will send synchronization events to other nodes in it's group.")
    static final String PRODUCER_PROPERTY = "producer";

    @Property(label = "Consumes Events", unbounded = PropertyUnbounded.ARRAY, description = "Whether or not this node will consume synchronization events sent from other nodes in it's group.")
    static final String CONSUMER_PROPERTY = "consumer";

    @Property(label = "Enabled Events", options = {
        @PropertyOption(name = "org.apache.karaf.cellar.bundle.BundleEventHandler", value = "Bundle Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.config.ConfigurationEventHandler", value = "Configuration Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.features.FeaturesEventHandler", value = "Feature Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.dosgi.RemoteServiceCallHandler", value = "DOSGi Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.event.ClusterEventHandler", value = "Cluster Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.obr.ObrBundleEventHandler", value = "OBR Bundle Events"),
        @PropertyOption(name = "org.apache.karaf.cellar.obr.ObrUrlEventHandler", value = "OBR Events")
    })
    static final String ENABLED_EVENTS_PROPERTY = "enabled.events";

    Set<String> getGroupNames();

    boolean isProducer();

    boolean isConsumer();

    Set<String> getEnabledEventHandlers();
}
