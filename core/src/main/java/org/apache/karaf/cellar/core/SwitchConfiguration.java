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

/**
 *
 * @author rmoquin
 */
public interface SwitchConfiguration {

    /**
     * @return the consumer
     */
    boolean isConsumer();

    /**
     * @return the enableBundleEvents
     */
    boolean isEnableBundleEvents();

    /**
     * @return the enableClusterEvents
     */
    boolean isEnableClusterEvents();

    /**
     * @return the enableConfigurationEvents
     */
    boolean isEnableConfigurationEvents();

    /**
     * @return the enableDOSGIEvents
     */
    boolean isEnableDOSGIEvents();

    /**
     * @return the enableFeatureEvents
     */
    boolean isEnableFeatureEvents();

    /**
     * @return the enableOBRBundleEvents
     */
    boolean isEnableOBRBundleEvents();

    /**
     * @return the enableObrEvents
     */
    boolean isEnableObrEvents();

    /**
     * @return the producer
     */
    boolean isProducer();

}
