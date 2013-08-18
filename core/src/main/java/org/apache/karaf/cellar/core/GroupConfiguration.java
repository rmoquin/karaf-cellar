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

/**
 *
 * @author rmoquin
 */
public interface GroupConfiguration {    
    String getName();
    
    boolean isSyncConfiguration();

    Set<String> getInboundConfigurationWhitelist();

    Set<String> getOutboundConfigurationWhitelist();

    Set<String> getInboundConfigurationBlacklist();

    Set<String> getOutboundConfigurationBlacklist();

    boolean isSyncFeatures();

    boolean isSyncFeatureRepos();

    Set<String> getInboundFeatureWhitelist();

    Set<String> getOutboundFeatureWhitelist();

    Set<String> getInboundFeatureBlacklist();

    Set<String> getOutboundFeatureBlacklist();

    boolean isSyncBundles();

    Set<String> getInboundBundleWhitelist();

    Set<String> getOutboundBundleWhitelist();

    Set<String> getInboundBundleBlacklist();

    Set<String> getOutboundBundleBlacklist();

    boolean isSyncOBRUrls();

    boolean isSyncOBRBundles();

    Set<String> getInboundOBRUrlsWhitelist();

    Set<String> getOutboundOBRUrlsWhitelist();

    Set<String> getInboundOBRUrlsBlacklist();

    Set<String> getOutboundOBRUrlsBlacklist();

    /**
     * Registers this node for producing and consuming messages between nodes in the group.
     *
     * @return the group object this configuration represents.
     */
    Group register();
}
