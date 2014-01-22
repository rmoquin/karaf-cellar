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
package org.apache.karaf.cellar.features.shell;

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.Result;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

@Command(scope = "cluster", name = "feature-uninstall", description = "Uninstall a feature from a cluster group")
public class UninstallFeatureCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "feature", description = "The feature name", required = true, multiValued = false)
    String feature;

    @Argument(index = 2, name = "version", description = "The feature version", required = false, multiValued = false)
    String version;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        if (executionContext.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF for this node");
            return null;
        }

        // check if the feature exists in the map
        if (!featureExists(groupName, feature, version)) {
            if (version != null) {
                System.err.println("Feature " + feature + "/" + version + " doesn't exist in the cluster group " + groupName);
            } else {
                System.err.println("Feature " + feature + " doesn't exist in the cluster group " + groupName);
            }
            return null;
        }

        // check if the outbound event is allowed
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
        Set<String> whitelist = groupConfig.getOutboundFeatureWhitelist();
        Set<String> blacklist = groupConfig.getOutboundFeatureBlacklist();
        if (cellarSupport.isAllowed(feature, whitelist, blacklist)) {
            System.err.println("Feature " + feature + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        // update the features in the cluster group
        updateFeatureStatus(groupName, feature, version, false);

        // broadcast the cluster event
        ClusterFeaturesEvent event = new ClusterFeaturesEvent(feature, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setForce(true);
        event.setSourceGroup(group);
        Map<Node, Result> responses = executionContext.executeAndWait(event, group.getNodes());
        printTaskResults(responses);
        return null;
    }
}
