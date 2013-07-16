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
@Meta.OCD(name = "Groups", description = "The list of groups that the current node belongs to and the rules for synchronizing with other nodes in the same group(s).")
public interface GroupMembershipConfig {
    @Meta.AD(name = "Group Names", deflt = "default", description = "The groups this node participates in.")
    Set<String> groupNames();

    @Meta.AD(name = "Sync Configuration", deflt = "true", description = "Enable synchronizing configuration changes to and from other nodes in the same group.")
    boolean syncConfiguration();

    @Meta.AD(name = "Whitelist Inbound Configurations", deflt = "*", description = "The set of configurations this node will accept synchronization changes for from other group nodes.")
    Set<String> inboundConfigurationWhitelist();

    @Meta.AD(name = "Whitelist Outbound Configurations", deflt = "*", description = "The set of configurations this node will send synchronization events for to other group nodes.")
    Set<String> outboundConfigurationWhitelist();

    @Meta.AD(name = "Blacklist Inbound Configurations", deflt = "org.apache.felix.fileinstall*,org.apache.karaf.cellar*,org.apache.karaf.management,org.apache.karaf.shell,org.ops4j.pax.logging,org.ops4j.pax.web",
            description = "The list of configurations this node will not accept synchronization events for from other group nodes.")
    Set<String> inboundConfigurationBlacklist();

    @Meta.AD(name = "Blacklist Outbound Configurations", deflt = "org.apache.felix.fileinstall*,org.apache.karaf.cellar*,org.apache.karaf.management,org.apache.karaf.shell,org.ops4j.pax.logging,org.ops4j.pax.web",
            description = "The list of configurations this node will not send synchronization events for to other group nodes.")
    Set<String> outboundConfigurationBlacklist();

    @Meta.AD(name = "Features", deflt = "true", description = "Enable synchronizing features changes to and from other nodes in the same group.")
    boolean syncFeatures();

    @Meta.AD(name = "Feature Repos", deflt = "true", description = "Enable synchronizing feature repository changes to and from other nodes in the same group.")
    boolean syncFeatureRepos();

    @Meta.AD(name = "Whitelist Inbound Features", deflt = "*", description = "The set of features this node will accept synchronization changes for from other group nodes.")
    Set<String> inboundFeatureWhitelist();

    @Meta.AD(name = "WhitelistOutbound Features", deflt = "*", description = "The set of features this node will send synchronization events for to other group nodes.")
    Set<String> outboundFeatureWhitelist();

    @Meta.AD(name = "Blacklist Inbound Features", deflt = "config,management,hazelcast,cellar*",
            description = "The list of features this node will not accept synchronization events for from other group nodes.")
    Set<String> inboundFeatureBlacklist();

    @Meta.AD(name = "Blacklist Outbound Features", deflt = "config,management,hazelcast,cellar*",
            description = "The list of features this node will not send synchronization events for to other group nodes.")
    Set<String> outboundFeatureBlacklist();

    @Meta.AD(name = "Bundles", deflt = "true", description = "Enable synchronizing bundle changes to and from other nodes in the same group.")
    boolean syncBundles();

    @Meta.AD(name = "Whitelist Inbound Bundles", deflt = "*",
            description = "The set of bundles this node will accept synchronization changes for from other group nodes.")
    Set<String> inboundBundleWhitelist();

    @Meta.AD(name = "Whitelist Outbound Bundles", deflt = "*",
            description = "The set of bundles this node will send synchronization events for to other group nodes.")
    Set<String> outboundBundleWhitelist();

    @Meta.AD(name = "Blacklist Inbound Bundles", deflt = "none",
            description = "The list of bundles this node will not accept synchronization events for from other group nodes.")
    Set<String> inboundBundleBlacklist();

    @Meta.AD(name = "Blacklist Outbound Bundles", deflt = "none",
            description = "The list of bundles this node will not send synchronization events for to other group nodes.")
    Set<String> outboundBundleBlacklist();

    @Meta.AD(name = "OBR Urls", deflt = "true", description = "Enable synchronizing OBR url changes to and from other nodes in the same group.")
    boolean syncOBRUrls();

    @Meta.AD(name = "OBR Bundles", deflt = "true", description = "Enable synchronizing OBR bundle changes to and from other nodes in the same group.")
    boolean syncOBRBundles();
}
