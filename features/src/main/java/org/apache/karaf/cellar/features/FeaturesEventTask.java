/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.features;

import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.features.Feature;

/**
 * Handler for cluster features event.
 */
public class FeaturesEventTask extends CommandHandler<FeatureEventCommand, FeatureEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesEventTask.class);
    private static final String separator = "/";

    private String id;
    private String name;
    private String version;
    private Boolean noClean;
    private Boolean noRefresh;
    private FeatureEvent.EventType type;
    private CellarSupport cellarSupport;

    public FeaturesEventTask() {
    }

    public FeaturesEventTask(String name, String version, FeatureEvent.EventType type) {
        this(name, version, false, false, type);
    }

    public FeaturesEventTask(String name, String version, Boolean noClean, Boolean noRefresh, FeatureEvent.EventType type) {
        this.id = name + separator + version;
        this.name = name;
        this.version = version;
        this.noClean = noClean;
        this.noRefresh = noRefresh;
        this.type = type;
    }

    /**
     * Handle a received cluster features event.
     *
     * @return
     */
    @Override
    public FeatureEventResponse execute(FeatureEventCommand command) {
        if (cellarSupport == null) {
            cellarSupport = new CellarSupport();
        }
        FeatureEventResponse result = new FeatureEventResponse();
        try {
            GroupManager groupManager = super.getService(GroupManager.class);
            FeaturesService featuresService = super.getService(FeaturesService.class);
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroup.getName());
            Set<String> whitelist = groupConfig.getInboundFeatureWhitelist();
            Set<String> blacklist = groupConfig.getInboundFeatureBlacklist();
            if (cellarSupport.isAllowed(name, whitelist, blacklist)) {
                boolean isInstalled = false;
                Feature[] localFeatures = featuresService.listInstalledFeatures();

                if (localFeatures != null && localFeatures.length > 0) {
                    for (Feature localFeature : localFeatures) {
                        if (localFeature.getName().equals(name) && (localFeature.getVersion().equals(version) || version == null)) {
                            isInstalled = true;
                            break;
                        }
                    }
                }

                if (FeatureEvent.EventType.FeatureInstalled.equals(type) && !isInstalled) {
                    EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);
                    if (noClean) {
                        options.add(FeaturesService.Option.NoCleanIfFailure);
                    }
                    if (noRefresh) {
                        options.add(FeaturesService.Option.NoAutoRefreshBundles);
                    }
                    if (version != null) {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", name, version);
                        featuresService.installFeature(name, version, options);
                    } else {
                        LOGGER.debug("CELLAR FEATURES: installing feature {}", name);
                        featuresService.installFeature(name, options);
                    }
                } else if (FeatureEvent.EventType.FeatureUninstalled.equals(type) && isInstalled) {
                    if (version != null) {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", name, version);
                        featuresService.uninstallFeature(name, version);
                    } else {
                        LOGGER.debug("CELLAR FEATURES: un-installing feature {}", name);
                        featuresService.uninstallFeature(name);
                    }
                }
                result.setSuccessful(true);
            } else {
                LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group", name, sourceGroup.getName());
                result.setSuccessful(false);
                result.setThrowable(new IllegalStateException("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group " + name));
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR FEATURES: failed to handle configuration task event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    public String getId() {
        this.id = name + separator + version;
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public Boolean getNoClean() {
        return this.noClean;
    }

    public void setNoClean(Boolean noClean) {
        this.noClean = noClean;
    }

    public Boolean getNoRefresh() {
        return noRefresh;
    }

    public void setNoRefresh(Boolean noRefresh) {
        this.noRefresh = noRefresh;
    }

    public FeatureEvent.EventType getType() {
        return type;
    }
}
