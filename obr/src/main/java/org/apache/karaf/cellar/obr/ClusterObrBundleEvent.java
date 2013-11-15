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
package org.apache.karaf.cellar.obr;

import java.util.Set;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster OBR bundle event.
 */
public class ClusterObrBundleEvent extends Event<ClusterObrEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrBundleEventHandler.class);

    protected static final char VERSION_DELIM = ',';

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.bundles.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private CellarSupport cellarSupport;
    private NodeConfiguration nodeConfiguration;
    private GroupManager groupManager;
    private String bundleId;
    private int type;
    private RepositoryAdmin obrService;

    public ClusterObrBundleEvent() {
    }

    public ClusterObrBundleEvent(String bundleId, int type) {
        this.bundleId = bundleId;
        this.type = type;
    }

    /**
     * Handle a received cluster OBR bundle event.
     */
    @Override
    protected ClusterObrEventResponse execute() throws Exception {
        // check if the handler is ON
//        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
//            LOGGER.debug("CELLAR OBR: {} switch is OFF", SWITCH_ID);
//            return;
//        }
        ClusterObrEventResponse response = new ClusterObrEventResponse();
        try {
            GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(sourceGroup.getName());
            Set<String> whitelist = groupConfig.getOutboundBundleWhitelist();
            Set<String> blacklist = groupConfig.getOutboundBundleBlacklist();
            if (cellarSupport.isAllowed(bundleId, whitelist, blacklist)) {
                Resolver resolver = getObrService().resolver();
                String[] target = getTarget(bundleId);
                Resource resource = selectNewestVersion(searchRepository(target[0], target[1]));
                if (resource != null) {
                    resolver.add(resource);
                } else {
                    LOGGER.warn("CELLAR OBR: bundle {} unknown", target[0]);
                }
                if ((resolver.getAddedResources() != null)
                        && (resolver.getAddedResources().length > 0)) {
                    if (resolver.resolve()) {
                        if (this.type == Constants.BUNDLE_START_EVENT_TYPE) {
                            resolver.deploy(Resolver.START);
                        } else {
                            resolver.deploy(0);
                        }
                    }
                } else {
                    Reason[] reqs = resolver.getUnsatisfiedRequirements();
                    if (reqs != null && reqs.length > 0) {
                        LOGGER.warn("CELLAR OBR: unsatisfied requirement(s): ");
                        for (Reason reason : reqs) {
                            LOGGER.warn("CELLAR OBR:    {}", reason.getRequirement().getFilter());
                            LOGGER.warn("CELLAR OBR:    {}", reason.getResource().getPresentationName());
                        }
                    } else {
                        LOGGER.warn("CELLAR OBR: could not resolve targets");
                    }
                }
            } else {
                LOGGER.debug("CELLAR OBR: bundle {} is marked as BLOCKED INBOUND for cluster group {}", bundleId, sourceGroup.getName());
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR OBR: failed to handle bundle event {}", bundleId, ex);
            response.setThrowable(ex);
            response.setSuccessful(false);
        }
        return response;
    }

    protected String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[]{bundle.substring(0, idx), bundle.substring(idx + 1)};
        } else {
            target = new String[]{bundle, null};
        }
        return target;
    }

    public Resource selectNewestVersion(Resource[] resources) {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++) {
            if (i == 0) {
                idx = 0;
                v = resources[i].getVersion();
            } else {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0) {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    protected Resource[] searchRepository(String targetId, String targetVersion) throws InvalidSyntaxException {
        try {
            Bundle bundle = getBundleContext().getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        } catch (NumberFormatException e) {
            // it was not a number, so ignore.
        }

        // the target ID may be a bundle name or a bundle symbolic name,
        // so create
        StringBuilder sb = new StringBuilder("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null) {
            sb.insert(0, "&(");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return getObrService().discoverResources(sb.toString());
    }

    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Boolean status = this.nodeConfiguration.getEnabledEvents().contains(this.getClass().getName());
            if (status) {
                eventSwitch.turnOn();
            } else {
                eventSwitch.turnOff();
            }
        } catch (Exception e) {
            // ignore
        }
        return this.eventSwitch;
    }

    /**
     * @return the cellarSupport
     */
    public CellarSupport getCellarSupport() {
        return cellarSupport;
    }

    /**
     * @param cellarSupport the cellarSupport to set
     */
    public void setCellarSupport(CellarSupport cellarSupport) {
        this.cellarSupport = cellarSupport;
    }

    /**
     * @return the groupManager
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * @param groupManager the groupManager to set
     */
    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /**
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }

    /**
     * @return the obrService
     */
    public RepositoryAdmin getObrService() {
        return obrService;
    }

    /**
     * @param obrService the obrService to set
     */
    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

    public String getBundleId() {
        return this.bundleId;
    }

    public int getType() {
        return this.type;
    }

}
