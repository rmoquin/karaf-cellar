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

import java.util.List;

/**
 *
 * @author rmoquin
 */
public interface GroupConfiguration {    
    String getName();
    
    boolean isSyncConfiguration();

    List<String> getInboundConfigurationWhitelist();

    List<String> getOutboundConfigurationWhitelist();

    List<String> getInboundConfigurationBlacklist();

    List<String> getOutboundConfigurationBlacklist();

    boolean isSyncFeatures();

    boolean isSyncFeatureRepos();

    List<String> getInboundFeatureWhitelist();

    List<String> getOutboundFeatureWhitelist();

    List<String> getInboundFeatureBlacklist();

    List<String> getOutboundFeatureBlacklist();

    boolean isSyncBundles();

    List<String> getInboundBundleWhitelist();

    List<String> getOutboundBundleWhitelist();

    List<String> getInboundBundleBlacklist();

    List<String> getOutboundBundleBlacklist();

    boolean isSyncOBRUrls();

    boolean isSyncOBRBundles();

    /**
     * Registers this node for producing and consuming messages between nodes in the group.
     *
     * @return the group object this configuration represents.
     */
    Group register();

    /**
     * @return the group
     */
    Group getGroup();
}
