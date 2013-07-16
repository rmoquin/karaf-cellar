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
public interface SynchronizationRules {
    /**
     * @return the inboundBundleBlacklist
     */
    List<String> getInboundBundleBlacklist();

    /**
     * @return the inboundBundleWhitelist
     */
    List<String> getInboundBundleWhitelist();

    /**
     * @return the inboundConfigurationBlacklist
     */
    List<String> getInboundConfigurationBlacklist();

    /**
     * @return the inboundConfigurationWhitelist
     */
    List<String> getInboundConfigurationWhitelist();

    /**
     * @return the inboundFeatureBlacklist
     */
    List<String> getInboundFeatureBlacklist();

    /**
     * @return the inboundFeatureWhitelist
     */
    List<String> getInboundFeatureWhitelist();

    /**
     * @return the name
     */
    String getName();

    /**
     * @return the outboundBundleBlacklist
     */
    List<String> getOutboundBundleBlacklist();

    /**
     * @return the outboundBundleWhitelist
     */
    List<String> getOutboundBundleWhitelist();

    /**
     * @return the outboundConfigurationBlacklist
     */
    List<String> getOutboundConfigurationBlacklist();

    /**
     * @return the outboundConfigurationWhitelist
     */
    List<String> getOutboundConfigurationWhitelist();

    /**
     * @return the outboundFeatureBlacklist
     */
    List<String> getOutboundFeatureBlacklist();

    /**
     * @return the outboundFeatureWhitelist
     */
    List<String> getOutboundFeatureWhitelist();

    /**
     * @return the syncConfiguration
     */
    boolean isSyncConfiguration();

    /**
     * @return the syncFeatureRepos
     */
    boolean isSyncFeatureRepos();

    /**
     * @return the syncFeatures
     */
    boolean isSyncFeatures();

    /**
     * @return the syncOBRBundles
     */
    boolean isSyncOBRBundles();

    /**
     * @return the syncOBRUrls
     */
    boolean isSyncOBRUrls();
}
