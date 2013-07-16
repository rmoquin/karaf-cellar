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

import aQute.bnd.annotation.metatype.Meta;
import java.util.Set;

/**
 *
 * @author rmoquin
 */
@Meta.OCD(name = "Node Synchronization Events", description = "Nodes in the same group are synchronized via events that communicate changes to a particular node.")
interface NodeEventConfig {
    @Meta.AD(name = "Produce Events", deflt = "true", description = "Whether or not synchronization events should be sent from this node when a change is made to it.")
    boolean producer();

    @Meta.AD(name = "Consume Events", deflt = "true", description = "Whether or not this node should consume synchronization events received from other nodes in the same group.")
    boolean consumer();

    @Meta.AD(name = "Event Types", optionLabels = { "Bundle Events", "Configuration Events", "Feature Events", "DOSGi Events", "Cluster Events", "OR Bundle Events", "OBR Events" },
            optionValues = { "org.apache.karaf.cellar.bundle.BundleEventHandler", "org.apache.karaf.cellar.config.ConfigurationEventHandler", "org.apache.karaf.cellar.features.FeaturesEventHandler", "org.apache.karaf.cellar.dosgi.RemoteServiceCallHandler", "org.apache.karaf.cellar.event.ClusterEventHandler", "org.apache.karaf.cellar.obr.ObrBundleEventHandler", "org.apache.karaf.cellar.obr.ObrUrlEventHandler" })
    Set<String> enabledEventHandlers();
}