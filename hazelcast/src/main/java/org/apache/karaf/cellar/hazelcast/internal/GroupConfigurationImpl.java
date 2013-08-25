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

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.Synchronizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
@Component(name = "org.apache.karaf.cellar.core.GroupConfiguration", createPid = false, configurationFactory = true,
        label = "Group Configuration", ds = false, metatype = true, description = "The configuration synchronization policies for a specific group.")
public class GroupConfigurationImpl implements GroupConfiguration {
    private static Logger LOGGER = LoggerFactory.getLogger(GroupConfigurationImpl.class);
    @Property(label = "Group Name", description = "The name of this group.")
    public static final String GROUP_NAME_PROPERTY = "name";
    @Property(label = "Whitelisted Configurations (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of configurations this node will accept synchronization changes for from other group nodes.")
    public static final String CONFIG_WHITELIST_INBOUND_PROPERTY = "inboundConfigurationWhitelist";
    @Property(label = "Whitelisted Configurations (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of configurations this node will send synchronization events for to other group nodes.")
    public static final String CONFIG_WHITELIST_OUTBOUND_PROPERTY = "outboundConfigurationWhitelist";
    @Property(label = "Whitelisted Configurations (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of configurations this node will accept synchronization events for from other group nodes.")
    public static final String CONFIG_BLACKLIST_INBOUND_PROPERTY = "inboundConfigurationBlacklist";
    @Property(label = "Whitelisted Configurations (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of configurations this node will not send synchronization events for to other group nodes.")
    public static final String CONFIG_BLACKLIST_OUTBOUND_PROPERTY = "outboundConfigurationBlacklist";
    @Property(label = "Sync Configurations", boolValue = true, description = "Enable synchronizing configuration changes to and from other nodes in the same group.")
    public static final String SYNC_CONFIGURATIONS_PROPERTY = "syncConfiguration";
    @Property(label = "Whitelisted Features (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of configurations this node will not send synchronization events for to other group nodes.")
    public static final String FEATURES_WHITELIST_INBOUND_PROPERTY = "inboundFeatureWhitelist";
    @Property(label = "Whitelisted Features (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of features this node will accept synchronization changes for from other group nodes.")
    public static final String FEATURES_WHITELIST_OUTBOUND_PROPERTY = "outboundFeatureWhitelist";
    @Property(label = "Blacklisted Features (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of features this node will not accept synchronization events for from other group nodes.")
    public static final String FEATURES_BLACKLIST_INBOUND_PROPERTY = "inboundFeatureBlacklist";
    @Property(label = "Blacklisted Features (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of features this node will not send synchronization events for to other group nodes.")
    public static final String FEATURES_BLACKLIST_OUTBOUND_PROPERTY = "outboundFeatureBlacklist";
    @Property(label = "Sync Features", boolValue = true, description = "Enable synchronizing features changes to and from other nodes in the same group.")
    public static final String SYNC_FEATURES_PROPERTY = "syncFeatures";
    @Property(label = "Sync Feature Repos", boolValue = true, description = "Enable synchronizing feature repository changes to and from other nodes in the same group.")
    public static final String SYNC_FEATURE_REPOS_PROPERTY = "syncFeatureRepos";
    @Property(label = "Whitelisted Bundles (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of bundles this node will accept synchronization changes for from other group nodes.")
    public static final String BUNDLES_WHITELIST_INBOUND_PROPERTY = "bundlesWhitelistInbound";
    @Property(label = "Whitelisted Bundles (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of bundles this node will send synchronization events for to other group nodes.")
    public static final String BUNDLES_WHITELIST_OUTBOUND_PROPERTY = "bundlesWhitelistOutbound";
    @Property(label = "Blacklisted Bundles (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of bundles this node will not accept synchronization events for from other group nodes.")
    public static final String BUNDLES_BLACKLIST_INBOUND_PROPERTY = "bundlesBlacklistInbound";
    @Property(label = "Blacklisted Bundles (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of bundles this node will not send synchronization events for to other group nodes.")
    public static final String BUNDLES_BLACKLIST_OUTBOUND_PROPERTY = "bundlesBlacklistOutbound";
    @Property(label = "Sync Bundles", boolValue = true, description = "Enable synchronizing bundle changes to and from other nodes in the same group.")
    public static final String SYNC_BUNDLES_PROPERTY = "syncBundles";
    @Property(label = "Whitelisted OBR URLs (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of OBR urls this node will accept synchronization changes for from other group nodes.")
    public static final String OBR_URLS_WHITELIST_INBOUND_PROPERTY = "obrUrlsWhitelistInbound";
    @Property(label = "Whitelisted OBR URLs (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The set of OBR urls this node will send synchronization events for to other group nodes.")
    public static final String OBR_URLS_WHITELIST_OUTBOUND_PROPERTY = "obrUrlsWhitelistOutbound";
    @Property(label = "Blacklisted OBR URLs (Inbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of OBR urls this node will not accept synchronization events for from other group nodes.")
    public static final String OBR_URLS_BLACKLIST_INBOUND_PROPERTY = "obrUrlsBlacklistInbound";
    @Property(label = "Blacklisted OBR URLs (Outbound)", unbounded = PropertyUnbounded.VECTOR, description = "The list of OBR urls this node will not send synchronization events for to other group nodes.")
    public static final String OBR_URLS_BLACKLIST_OUTBOUND_PROPERTY = "obrUrlsBlacklistOutbound";
    @Property(label = "Sync OBR Urls", boolValue = true, description = "Enable synchronizing OBR url changes to and from other nodes in the same group.")
    public static final String SYNC_OBR_URLS_PROPERTY = "syncOBRUrls";
    @Property(label = "Sync OBR Bundles", boolValue = true, description = "Enable synchronizing OBR bundle changes to and from other nodes in the same group.")
    public static final String SYNC_OBR_BUNDLES_PROPERTY = "syncOBRBundles";
    private Hashtable<String, Object> properties = new Hashtable<String, Object>();
    private CellarCluster masterCluster;
    private BundleContext bundleContext;

    public void init() {
    }

    public void destroy() {
    }

    /**
     * Registers this node for producing and consuming messages between nodes in the group.
     *
     * @return the group object this configuration represents.
     */
    @Override
    public Group register() {
        String groupName = this.getName();
        return new Group(groupName);
    }

    public void synchronizeNodes() {
        Group group = (Group) masterCluster.getMap(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE).get(this.getName());
        try {
            ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(Synchronizer.class.getCanonicalName(), null);
            if (serviceReferences != null && serviceReferences.length > 0) {
                for (ServiceReference ref : serviceReferences) {
                    Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                    if (synchronizer != null && synchronizer.isSyncEnabled(group)) {
                        synchronizer.pull(group);
                        synchronizer.push(group);
                    }
                    bundleContext.ungetService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("CELLAR HAZELCAST: failed to lookup available synchronizers", e);
        }
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return (String) this.properties.get(GROUP_NAME_PROPERTY);
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.properties.put(GROUP_NAME_PROPERTY, name);
    }

    /**
     * @return the syncConfiguration
     */
    @Override
    public boolean isSyncConfiguration() {
        return (Boolean) this.properties.get(SYNC_CONFIGURATIONS_PROPERTY);
    }

    /**
     * @param syncConfiguration the syncConfiguration to set
     */
    public void setSyncConfiguration(boolean syncConfiguration) {
        this.properties.put(SYNC_CONFIGURATIONS_PROPERTY, syncConfiguration);
    }

    /**
     * @return the inboundConfigurationWhitelist
     */
    @Override
    public Set<String> getInboundConfigurationWhitelist() {
        return (Set<String>) this.properties.get(CONFIG_WHITELIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundConfigurationWhitelist the inboundConfigurationWhitelist to set
     */
    public void setInboundConfigurationWhitelist(Set<String> inboundConfigurationWhitelist) {
        this.properties.put(CONFIG_WHITELIST_INBOUND_PROPERTY, inboundConfigurationWhitelist);
    }

    /**
     * @return the outboundConfigurationWhitelist
     */
    @Override
    public Set<String> getOutboundConfigurationWhitelist() {
        return (Set<String>) this.properties.get(CONFIG_WHITELIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundConfigurationWhitelist the outboundConfigurationWhitelist to set
     */
    public void setOutboundConfigurationWhitelist(Set<String> outboundConfigurationWhitelist) {
        this.properties.put(CONFIG_WHITELIST_OUTBOUND_PROPERTY, outboundConfigurationWhitelist);
    }

    /**
     * @return the inboundConfigurationBlacklist
     */
    @Override
    public Set<String> getInboundConfigurationBlacklist() {
        return (Set<String>) this.properties.get(CONFIG_BLACKLIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundConfigurationBlacklist the inboundConfigurationBlacklist to set
     */
    public void setInboundConfigurationBlacklist(Set<String> inboundConfigurationBlacklist) {
        this.properties.put(CONFIG_BLACKLIST_INBOUND_PROPERTY, inboundConfigurationBlacklist);
    }

    /**
     * @return the outboundConfigurationBlacklist
     */
    @Override
    public Set<String> getOutboundConfigurationBlacklist() {
        return (Set<String>) this.properties.get(CONFIG_BLACKLIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundConfigurationBlacklist the outboundConfigurationBlacklist to set
     */
    public void setOutboundConfigurationBlacklist(Set<String> outboundConfigurationBlacklist) {
        this.properties.put(CONFIG_BLACKLIST_OUTBOUND_PROPERTY, outboundConfigurationBlacklist);
    }

    /**
     * @return the syncFeatures
     */
    @Override
    public boolean isSyncFeatures() {
        return (Boolean) this.properties.get(SYNC_FEATURES_PROPERTY);
    }

    /**
     * @param syncFeatures the syncFeatures to set
     */
    public void setSyncFeatures(boolean syncFeatures) {
        this.properties.put(SYNC_FEATURES_PROPERTY, syncFeatures);
    }

    /**
     * @return the syncFeatureRepos
     */
    @Override
    public boolean isSyncFeatureRepos() {
        return (Boolean) this.properties.get(SYNC_FEATURE_REPOS_PROPERTY);
    }

    /**
     * @param syncFeatureRepos the syncFeatureRepos to set
     */
    public void setSyncFeatureRepos(boolean syncFeatureRepos) {
        this.properties.put(SYNC_FEATURE_REPOS_PROPERTY, syncFeatureRepos);
    }

    /**
     * @return the inboundFeatureWhitelist
     */
    @Override
    public Set<String> getInboundFeatureWhitelist() {
        return (Set<String>) this.properties.get(FEATURES_WHITELIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundFeatureWhitelist the inboundFeatureWhitelist to set
     */
    public void setInboundFeatureWhitelist(Set<String> inboundFeatureWhitelist) {
        this.properties.put(FEATURES_WHITELIST_INBOUND_PROPERTY, inboundFeatureWhitelist);
    }

    /**
     * @return the outboundFeatureWhitelist
     */
    @Override
    public Set<String> getOutboundFeatureWhitelist() {
        return (Set<String>) this.properties.get(FEATURES_WHITELIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundFeatureWhitelist the outboundFeatureWhitelist to set
     */
    public void setOutboundFeatureWhitelist(Set<String> outboundFeatureWhitelist) {
        this.properties.put(FEATURES_WHITELIST_OUTBOUND_PROPERTY, outboundFeatureWhitelist);
    }

    /**
     * @return the inboundFeatureBlacklist
     */
    @Override
    public Set<String> getInboundFeatureBlacklist() {
        return (Set<String>) this.properties.get(FEATURES_BLACKLIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundFeatureBlacklist the inboundFeatureBlacklist to set
     */
    public void setInboundFeatureBlacklist(Set<String> inboundFeatureBlacklist) {
        this.properties.put(FEATURES_BLACKLIST_INBOUND_PROPERTY, inboundFeatureBlacklist);
    }

    /**
     * @return the outboundFeatureBlacklist
     */
    @Override
    public Set<String> getOutboundFeatureBlacklist() {
        return (Set<String>) this.properties.get(FEATURES_BLACKLIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundFeatureBlacklist the outboundFeatureBlacklist to set
     */
    public void setOutboundFeatureBlacklist(Set<String> outboundFeatureBlacklist) {
        this.properties.put(FEATURES_BLACKLIST_OUTBOUND_PROPERTY, outboundFeatureBlacklist);
    }

    /**
     * @return the syncBundles
     */
    @Override
    public boolean isSyncBundles() {
        return (Boolean) this.properties.get(SYNC_BUNDLES_PROPERTY);
    }

    /**
     * @param syncBundles the syncBundles to set
     */
    public void setSyncBundles(boolean syncBundles) {
        this.properties.put(SYNC_BUNDLES_PROPERTY, syncBundles);
    }

    /**
     * @return the inboundBundleWhitelist
     */
    @Override
    public Set<String> getInboundBundleWhitelist() {
        return (Set<String>) this.properties.get(BUNDLES_WHITELIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundBundleWhitelist the inboundBundleWhitelist to set
     */
    public void setInboundBundleWhitelist(Set<String> inboundBundleWhitelist) {
        this.properties.put(BUNDLES_WHITELIST_INBOUND_PROPERTY, inboundBundleWhitelist);
    }

    /**
     * @return the outboundBundleWhitelist
     */
    @Override
    public Set<String> getOutboundBundleWhitelist() {
        return (Set<String>) this.properties.get(BUNDLES_WHITELIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundBundleWhitelist the outboundBundleWhitelist to set
     */
    public void setOutboundBundleWhitelist(Set<String> outboundBundleWhitelist) {
        this.properties.put(BUNDLES_WHITELIST_OUTBOUND_PROPERTY, outboundBundleWhitelist);
    }

    /**
     * @return the inboundBundleBlacklist
     */
    @Override
    public Set<String> getInboundBundleBlacklist() {
        return (Set<String>) this.properties.get(BUNDLES_BLACKLIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundBundleBlacklist the inboundBundleBlacklist to set
     */
    public void setInboundBundleBlacklist(Set<String> inboundBundleBlacklist) {
        this.properties.put(BUNDLES_BLACKLIST_INBOUND_PROPERTY, inboundBundleBlacklist);
    }

    /**
     * @return the outboundBundleBlacklist
     */
    @Override
    public Set<String> getOutboundBundleBlacklist() {
        return (Set<String>) this.properties.get(BUNDLES_BLACKLIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundBundleBlacklist the outboundBundleBlacklist to set
     */
    public void setOutboundBundleBlacklist(Set<String> outboundBundleBlacklist) {
        this.properties.put(BUNDLES_BLACKLIST_OUTBOUND_PROPERTY, outboundBundleBlacklist);
    }

    /**
     * @return the syncOBRUrls
     */
    @Override
    public boolean isSyncOBRUrls() {
        return (Boolean) this.properties.get(SYNC_OBR_URLS_PROPERTY);
    }

    /**
     * @param syncOBRUrls the syncOBRUrls to set
     */
    public void setSyncOBRUrls(boolean syncOBRUrls) {
        this.properties.put(SYNC_OBR_BUNDLES_PROPERTY, syncOBRUrls);
    }

    /**
     * @return the inboundOBRUrlsBlacklist
     */
    @Override
    public Set<String> getInboundOBRUrlsWhitelist() {
        return (Set<String>) this.properties.get(OBR_URLS_WHITELIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundOBRUrlsWhitelist the inboundBundleBlacklist to set
     */
    public void setInboundOBRUrlsWhitelist(Set<String> inboundOBRUrlsWhitelist) {
        this.properties.put(OBR_URLS_WHITELIST_INBOUND_PROPERTY, inboundOBRUrlsWhitelist);
    }

    /**
     * @return the outboundOBRUrlsBlacklist
     */
    @Override
    public Set<String> getOutboundOBRUrlsWhitelist() {
        return (Set<String>) this.properties.get(OBR_URLS_WHITELIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundOBRUrlsWhitelist the outboundBundleWhitelist to set
     */
    public void setOutboundOBRUrlsWhitelist(Set<String> outboundOBRUrlsWhitelist) {
        this.properties.put(OBR_URLS_WHITELIST_OUTBOUND_PROPERTY, outboundOBRUrlsWhitelist);
    }

    /**
     * @return the inboundOBRUrlsBlacklist
     */
    @Override
    public Set<String> getInboundOBRUrlsBlacklist() {
        return (Set<String>) this.properties.get(OBR_URLS_BLACKLIST_INBOUND_PROPERTY);
    }

    /**
     * @param inboundOBRUrlsBlacklist the inboundBundleBlacklist to set
     */
    public void setInboundOBRUrlsBlacklist(Set<String> inboundOBRUrlsBlacklist) {
        this.properties.put(OBR_URLS_BLACKLIST_INBOUND_PROPERTY, inboundOBRUrlsBlacklist);
    }

    /**
     * @return the outboundOBRUrlsBlacklist
     */
    @Override
    public Set<String> getOutboundOBRUrlsBlacklist() {
        return (Set<String>) this.properties.get(OBR_URLS_BLACKLIST_OUTBOUND_PROPERTY);
    }

    /**
     * @param outboundOBRUrlsBlacklist the outboundBundleBlacklist to set
     */
    public void setOutboundOBRUrlsBlacklist(Set<String> outboundOBRUrlsBlacklist) {
        this.properties.put(OBR_URLS_BLACKLIST_OUTBOUND_PROPERTY, outboundOBRUrlsBlacklist);
    }

    /**
     * @return the syncOBRBundles
     */
    @Override
    public boolean isSyncOBRBundles() {
        return (Boolean) this.properties.get(SYNC_OBR_BUNDLES_PROPERTY);
    }

    /**
     * @param syncOBRBundles the syncOBRBundles to set
     */
    public void setSyncOBRBundles(boolean syncOBRBundles) {
        this.properties.put(SYNC_OBR_BUNDLES_PROPERTY, syncOBRBundles);
    }

    /**
     * @return the properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @return the masterCluster
     */
    public CellarCluster getMasterCluster() {
        return masterCluster;
    }

    /**
     * @param masterCluster the masterCluster to set
     */
    public void setMasterCluster(CellarCluster masterCluster) {
        this.masterCluster = masterCluster;
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
