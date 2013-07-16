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

import java.util.Set;
import org.apache.karaf.cellar.core.SwitchConfiguration;

/**
 *
 * @author rmoquin
 */
public class HazelcastSwitchConfiguration implements SwitchConfiguration {
    private Set<String> groupNames;
    private boolean producer;
    private boolean consumer;
    private boolean enableBundleEvents;
    private boolean enableConfigurationEvents;
    private boolean enableFeatureEvents;
    private boolean enableDOSGIEvents;
    private boolean enableClusterEvents;
    private boolean enableOBRBundleEvents;
    private boolean enableObrEvents;

    /**
     * @return the producer
     */
    @Override
    public boolean isProducer() {
        return producer;
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer(boolean producer) {
        this.producer = producer;
    }

    /**
     * @return the consumer
     */
    @Override
    public boolean isConsumer() {
        return consumer;
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(boolean consumer) {
        this.consumer = consumer;
    }

    /**
     * @return the enableBundleEvents
     */
    @Override
    public boolean isEnableBundleEvents() {
        return enableBundleEvents;
    }

    /**
     * @param enableBundleEvents the enableBundleEvents to set
     */
    public void setEnableBundleEvents(boolean enableBundleEvents) {
        this.enableBundleEvents = enableBundleEvents;
    }

    /**
     * @return the enableConfigurationEvents
     */
    @Override
    public boolean isEnableConfigurationEvents() {
        return enableConfigurationEvents;
    }

    /**
     * @param enableConfigurationEvents the enableConfigurationEvents to set
     */
    public void setEnableConfigurationEvents(boolean enableConfigurationEvents) {
        this.enableConfigurationEvents = enableConfigurationEvents;
    }

    /**
     * @return the enableFeatureEvents
     */
    @Override
    public boolean isEnableFeatureEvents() {
        return enableFeatureEvents;
    }

    /**
     * @param enableFeatureEvents the enableFeatureEvents to set
     */
    public void setEnableFeatureEvents(boolean enableFeatureEvents) {
        this.enableFeatureEvents = enableFeatureEvents;
    }

    /**
     * @return the enableDOSGIEvents
     */
    @Override
    public boolean isEnableDOSGIEvents() {
        return enableDOSGIEvents;
    }

    /**
     * @param enableDOSGIEvents the enableDOSGIEvents to set
     */
    public void setEnableDOSGIEvents(boolean enableDOSGIEvents) {
        this.enableDOSGIEvents = enableDOSGIEvents;
    }

    /**
     * @return the enableClusterEvents
     */
    @Override
    public boolean isEnableClusterEvents() {
        return enableClusterEvents;
    }

    /**
     * @param enableClusterEvents the enableClusterEvents to set
     */
    public void setEnableClusterEvents(boolean enableClusterEvents) {
        this.enableClusterEvents = enableClusterEvents;
    }

    /**
     * @return the enableOBRBundleEvents
     */
    @Override
    public boolean isEnableOBRBundleEvents() {
        return enableOBRBundleEvents;
    }

    /**
     * @param enableOBRBundleEvents the enableOBRBundleEvents to set
     */
    public void setEnableOBRBundleEvents(boolean enableOBRBundleEvents) {
        this.enableOBRBundleEvents = enableOBRBundleEvents;
    }

    /**
     * @return the enableObrEvents
     */
    @Override
    public boolean isEnableObrEvents() {
        return enableObrEvents;
    }

    /**
     * @param enableObrEvents the enableObrEvents to set
     */
    public void setEnableObrEvents(boolean enableObrEvents) {
        this.enableObrEvents = enableObrEvents;
    }

    /**
     * @return the groupNames
     */
    public Set<String> getGroupNames() {
        return groupNames;
    }

    /**
     * @param groupNames the groupNames to set
     */
    public void setGroupNames(Set<String> groupNames) {
        this.groupNames = groupNames;
    }
}
