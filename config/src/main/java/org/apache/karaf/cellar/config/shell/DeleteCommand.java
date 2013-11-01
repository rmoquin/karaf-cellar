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
package org.apache.karaf.cellar.config.shell;

import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.osgi.service.cm.ConfigurationEvent;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.karaf.cellar.config.ConfigurationEventTask;
import org.apache.karaf.cellar.core.GroupConfiguration;

@Command(scope = "cluster", name = "config-delete", description = "Delete a configuration from a cluster group")
public class DeleteCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID", required = true, multiValued = false)
    String pid;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

		//TODO This needs to be re-enabled
        /*if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
         System.err.println("Cluster event producer is OFF");
         return null;
         }*/
        // check if the config pid is allowed
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(group.getName());
        Set<String> whitelist = groupConfig.getOutboundBundleWhitelist();
        Set<String> blacklist = groupConfig.getOutboundBundleBlacklist();
        if (!isAllowed(pid, whitelist, blacklist)) {
            System.err.println("Configuration PID " + pid + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (clusterConfigurations != null) {
            // update configurations in the cluster group
            clusterConfigurations.remove(pid);

            // broadcast a cluster event
            ConfigurationEventTask event = new ConfigurationEventTask();
            event.setSourceGroup(group);
            event.setType(ConfigurationEvent.CM_DELETED);
            executionContext.execute(event, group.getNodesExcluding(groupManager.getNode()));

        } else {
            System.out.println("Configuration distributed map not found for cluster group " + groupName);
        }

        return null;
    }

}
