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

import java.util.List;
import org.apache.karaf.cellar.core.SynchronizationRules;

/**
 *
 * @author rmoquin
 */
public class HazelcastSynchronizationRules implements SynchronizationRules {
    private String name;
    private boolean syncConfiguration;
    private List<String> inboundConfigurationWhitelist;
    private List<String> outboundConfigurationWhitelist;
    private List<String> inboundConfigurationBlacklist;
    private List<String> outboundConfigurationBlacklist;
    private boolean syncFeatures;
    private boolean syncFeatureRepos;
    private List<String> inboundFeatureWhitelist;
    private List<String> outboundFeatureWhitelist;
    private List<String> inboundFeatureBlacklist;
    private List<String> outboundFeatureBlacklist;
    private boolean syncBundles;
    private List<String> inboundBundleWhitelist;
    private List<String> outboundBundleWhitelist;
    private List<String> inboundBundleBlacklist;
    private List<String> outboundBundleBlacklist;
    private boolean syncOBRUrls;
    private boolean syncOBRBundles;

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the syncConfiguration
     */
    @Override
    public boolean isSyncConfiguration() {
        return syncConfiguration;
    }

    /**
     * @param syncConfiguration the syncConfiguration to set
     */
    public void setSyncConfiguration(boolean syncConfiguration) {
        this.syncConfiguration = syncConfiguration;
    }

    /**
     * @return the inboundConfigurationWhitelist
     */
    @Override
    public List<String> getInboundConfigurationWhitelist() {
        return inboundConfigurationWhitelist;
    }

    /**
     * @param inboundConfigurationWhitelist the inboundConfigurationWhitelist to set
     */
    public void setInboundConfigurationWhitelist(List<String> inboundConfigurationWhitelist) {
        this.inboundConfigurationWhitelist = inboundConfigurationWhitelist;
    }

    /**
     * @return the outboundConfigurationWhitelist
     */
    @Override
    public List<String> getOutboundConfigurationWhitelist() {
        return outboundConfigurationWhitelist;
    }

    /**
     * @param outboundConfigurationWhitelist the outboundConfigurationWhitelist to set
     */
    public void setOutboundConfigurationWhitelist(List<String> outboundConfigurationWhitelist) {
        this.outboundConfigurationWhitelist = outboundConfigurationWhitelist;
    }

    /**
     * @return the inboundConfigurationBlacklist
     */
    @Override
    public List<String> getInboundConfigurationBlacklist() {
        return inboundConfigurationBlacklist;
    }

    /**
     * @param inboundConfigurationBlacklist the inboundConfigurationBlacklist to set
     */
    public void setInboundConfigurationBlacklist(List<String> inboundConfigurationBlacklist) {
        this.inboundConfigurationBlacklist = inboundConfigurationBlacklist;
    }

    /**
     * @return the outboundConfigurationBlacklist
     */
    @Override
    public List<String> getOutboundConfigurationBlacklist() {
        return outboundConfigurationBlacklist;
    }

    /**
     * @param outboundConfigurationBlacklist the outboundConfigurationBlacklist to set
     */
    public void setOutboundConfigurationBlacklist(List<String> outboundConfigurationBlacklist) {
        this.outboundConfigurationBlacklist = outboundConfigurationBlacklist;
    }

    /**
     * @return the syncFeatures
     */
    @Override
    public boolean isSyncFeatures() {
        return syncFeatures;
    }

    /**
     * @param syncFeatures the syncFeatures to set
     */
    public void setSyncFeatures(boolean syncFeatures) {
        this.syncFeatures = syncFeatures;
    }

    /**
     * @return the syncFeatureRepos
     */
    @Override
    public boolean isSyncFeatureRepos() {
        return syncFeatureRepos;
    }

    /**
     * @param syncFeatureRepos the syncFeatureRepos to set
     */
    public void setSyncFeatureRepos(boolean syncFeatureRepos) {
        this.syncFeatureRepos = syncFeatureRepos;
    }

    /**
     * @return the inboundFeatureWhitelist
     */
    @Override
    public List<String> getInboundFeatureWhitelist() {
        return inboundFeatureWhitelist;
    }

    /**
     * @param inboundFeatureWhitelist the inboundFeatureWhitelist to set
     */
    public void setInboundFeatureWhitelist(List<String> inboundFeatureWhitelist) {
        this.inboundFeatureWhitelist = inboundFeatureWhitelist;
    }

    /**
     * @return the outboundFeatureWhitelist
     */
    @Override
    public List<String> getOutboundFeatureWhitelist() {
        return outboundFeatureWhitelist;
    }

    /**
     * @param outboundFeatureWhitelist the outboundFeatureWhitelist to set
     */
    public void setOutboundFeatureWhitelist(List<String> outboundFeatureWhitelist) {
        this.outboundFeatureWhitelist = outboundFeatureWhitelist;
    }

    /**
     * @return the inboundFeatureBlacklist
     */
    @Override
    public List<String> getInboundFeatureBlacklist() {
        return inboundFeatureBlacklist;
    }

    /**
     * @param inboundFeatureBlacklist the inboundFeatureBlacklist to set
     */
    public void setInboundFeatureBlacklist(List<String> inboundFeatureBlacklist) {
        this.inboundFeatureBlacklist = inboundFeatureBlacklist;
    }

    /**
     * @return the outboundFeatureBlacklist
     */
    @Override
    public List<String> getOutboundFeatureBlacklist() {
        return outboundFeatureBlacklist;
    }

    /**
     * @param outboundFeatureBlacklist the outboundFeatureBlacklist to set
     */
    public void setOutboundFeatureBlacklist(List<String> outboundFeatureBlacklist) {
        this.outboundFeatureBlacklist = outboundFeatureBlacklist;
    }

    /**
     * @return the syncBundles
     */
    public boolean isSyncBundles() {
        return syncBundles;
    }

    /**
     * @param syncBundles the syncBundles to set
     */
    public void setSyncBundles(boolean syncBundles) {
        this.syncBundles = syncBundles;
    }

    /**
     * @return the inboundBundleWhitelist
     */
    @Override
    public List<String> getInboundBundleWhitelist() {
        return inboundBundleWhitelist;
    }

    /**
     * @param inboundBundleWhitelist the inboundBundleWhitelist to set
     */
    public void setInboundBundleWhitelist(List<String> inboundBundleWhitelist) {
        this.inboundBundleWhitelist = inboundBundleWhitelist;
    }

    /**
     * @return the outboundBundleWhitelist
     */
    @Override
    public List<String> getOutboundBundleWhitelist() {
        return outboundBundleWhitelist;
    }

    /**
     * @param outboundBundleWhitelist the outboundBundleWhitelist to set
     */
    public void setOutboundBundleWhitelist(List<String> outboundBundleWhitelist) {
        this.outboundBundleWhitelist = outboundBundleWhitelist;
    }

    /**
     * @return the inboundBundleBlacklist
     */
    @Override
    public List<String> getInboundBundleBlacklist() {
        return inboundBundleBlacklist;
    }

    /**
     * @param inboundBundleBlacklist the inboundBundleBlacklist to set
     */
    public void setInboundBundleBlacklist(List<String> inboundBundleBlacklist) {
        this.inboundBundleBlacklist = inboundBundleBlacklist;
    }

    /**
     * @return the outboundBundleBlacklist
     */
    @Override
    public List<String> getOutboundBundleBlacklist() {
        return outboundBundleBlacklist;
    }

    /**
     * @param outboundBundleBlacklist the outboundBundleBlacklist to set
     */
    public void setOutboundBundleBlacklist(List<String> outboundBundleBlacklist) {
        this.outboundBundleBlacklist = outboundBundleBlacklist;
    }

    /**
     * @return the syncOBRUrls
     */
    @Override
    public boolean isSyncOBRUrls() {
        return syncOBRUrls;
    }

    /**
     * @param syncOBRUrls the syncOBRUrls to set
     */
    public void setSyncOBRUrls(boolean syncOBRUrls) {
        this.syncOBRUrls = syncOBRUrls;
    }

    /**
     * @return the syncOBRBundles
     */
    @Override
    public boolean isSyncOBRBundles() {
        return syncOBRBundles;
    }

    /**
     * @param syncOBRBundles the syncOBRBundles to set
     */
    public void setSyncOBRBundles(boolean syncOBRBundles) {
        this.syncOBRBundles = syncOBRBundles;
    }
}
