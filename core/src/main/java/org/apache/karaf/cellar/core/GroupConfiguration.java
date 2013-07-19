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
import org.apache.felix.scr.annotations.PropertyUnbounded;

/**
 *
 * @author rmoquin
 */
@Component(name = "Group Configuration", description = "The configuration synchronization policies for a specific group.")
public interface GroupConfiguration {
    @Property(label = "Group Name", description = "The name of this group.")
    static final String GROUP_NAME_PROPERTY = "group.name";
    
    @Property(label = "Whitelisted Configurations (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The set of configurations this node will accept synchronization changes for from other group nodes.")
    static final String CONFIG_WHITELIST_INBOUND_PROPERTY = "config.whitelist.inbound";

    @Property(label = "Whitelisted Configurations (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The set of configurations this node will send synchronization events for to other group nodes.")
    static final String CONFIG_WHITELIST_OUTBOUND_PROPERTY = "config.whitelist.outbound";

    @Property(label = "Whitelisted Configurations (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of configurations this node will accept synchronization events for from other group nodes.")
    static final String CONFIG_BLACKLIST_INBOUND_PROPERTY = "config.blacklist.inbound";

    @Property(label = "Whitelisted Configurations (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of configurations this node will not send synchronization events for to other group nodes.")
    static final String CONFIG_BLACKLIST_OUTBOUND_PROPERTY = "config.blacklist.outbound";

    @Property(label = "Sync Configurations", boolValue = true, description = "Enable synchronizing configuration changes to and from other nodes in the same group.")
    static final String SYNC_CONFIGURATIONS_PROPERTY = "configs.sync";

    @Property(label = "Whitelisted Features (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of configurations this node will not send synchronization events for to other group nodes.")
    static final String FEATURES_WHITELIST_INBOUND_PROPERTY = "features.whitelist.inbound";

    @Property(label = "Whitelisted Features (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The set of features this node will accept synchronization changes for from other group nodes.")
    static final String FEATURES_WHITELIST_OUTBOUND_PROPERTY = "features.whitelist.outbound";

    @Property(label = "Blacklisted Features (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of features this node will not accept synchronization events for from other group nodes.")
    static final String FEATURES_BLACKLIST_INBOUND_PROPERTY = "features.blacklist.inbound";

    @Property(label = "Blacklisted Features (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of features this node will not send synchronization events for to other group nodes.")
    static final String FEATURES_BLACKLIST_OUTBOUND_PROPERTY = "features.blacklist.outbound";

    @Property(label = "Sync Features", boolValue = true, description = "Enable synchronizing features changes to and from other nodes in the same group.")
    static final String SYNC_FEATURES_PROPERTY = "features.sync";

    @Property(label = "Sync Feature Repos", boolValue = true, description = "Enable synchronizing feature repository changes to and from other nodes in the same group.")
    static final String SYNC_FEATURE_REPOS_PROPERTY = "features.repositories.sync";

    @Property(label = "Whitelisted Bundles (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The set of bundles this node will accept synchronization changes for from other group nodes.")
    static final String BUNDLES_WHITELIST_INBOUND_PROPERTY = "bundles.whitelist.inbound";

    @Property(label = "Whitelisted Bundles (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The set of bundles this node will send synchronization events for to other group nodes.")
    static final String BUNDLES_WHITELIST_OUTBOUND_PROPERTY = "bundles.whitelist.outbound";

    @Property(label = "Blacklisted Bundles (Inbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of bundles this node will not accept synchronization events for from other group nodes.")
    static final String BUNDLES_BLACKLIST_INBOUND_PROPERTY = "bundles.blacklist.inbound";

    @Property(label = "Blacklisted Bundles (Outbound)", unbounded = PropertyUnbounded.ARRAY, description = "The list of bundles this node will not send synchronization events for to other group nodes.")
    static final String BUNDLES_BLACKLIST_OUTBOUND_PROPERTY = "bundles.blacklist.outbound";

    @Property(label = "Sync Bundles", boolValue = true, description = "Enable synchronizing bundle changes to and from other nodes in the same group.")
    static final String SYNC_BUNDLES_PROPERTY = "bundles.sync";

    @Property(label = "Sync OBR Urls", boolValue = true, description = "Enable synchronizing OBR url changes to and from other nodes in the same group.")
    static final String SYNC_OBR_URLS_PROPERTY = "obr.urls.sync";

    @Property(label = "Sync OBR Bundles", boolValue = true, description = "Enable synchronizing OBR bundle changes to and from other nodes in the same group.")
    static final String SYNC_OBR_BUNDLES_PROPERTY = "obr.bundles.sync";
    
    String getGroupName();
    
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
}
