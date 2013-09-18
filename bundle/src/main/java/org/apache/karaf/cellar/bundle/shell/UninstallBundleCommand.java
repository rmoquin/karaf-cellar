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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.shell.commands.Command;
import org.osgi.framework.BundleEvent;

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.bundle.BundleEventTask;
import org.apache.karaf.cellar.core.GroupConfiguration;

@Command(scope = "cluster", name = "bundle-uninstall", description = "Uninstall a bundle from a cluster group")
public class UninstallBundleCommand extends BundleCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // update the bundle in the cluster group
        String location;
        String key = null;
        Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

        key = selector(clusterBundles);

        if (key == null) {
            System.err.println("Bundle " + key + " not found in cluster group " + groupName);
            return null;
        }

        BundleState state = clusterBundles.get(key);
        if (state == null) {
            System.err.println("Bundle " + key + " not found in cluster group " + groupName);
            return null;
        }
        location = state.getLocation();

        // check if the bundle is allowed
        CellarSupport support = new CellarSupport();
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
        Set<String> whitelist = groupConfig.getOutboundBundleWhitelist();
        Set<String> blacklist = groupConfig.getOutboundBundleBlacklist();
        if (!support.isAllowed(location, whitelist, blacklist)) {
            System.err.println("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        clusterBundles.remove(key);

        // broadcast the cluster event
        String[] split = key.split("/");
        BundleEventTask event = new BundleEventTask(split[0], split[1], location, BundleEvent.UNINSTALLED);
        event.setSourceGroup(group);
        executionContext.executeAndWait(event, group.getNodesExcluding(groupManager.getNode()));

        return null;
    }
}
