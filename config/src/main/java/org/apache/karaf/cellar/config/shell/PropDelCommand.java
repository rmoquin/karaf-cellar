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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.karaf.cellar.config.ConfigurationEventTask;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;

@Command(scope = "cluster", name = "config-propdel", description = "Delete a property from a configuration in a cluster group")
public class PropDelCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID", required = true, multiValued = false)
    String pid;

    @Argument(index = 2, name = "key", description = "The property key to delete", required = true, multiValued = false)
    String key;

    private DistributedExecutionContext executionContext;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // check if the configuration PID is allowed
        GroupConfiguration groupConfig = groupManager.findGroupConfigurationByName(groupName);
        Set<String> whitelist = groupConfig.getOutboundConfigurationWhitelist();
        Set<String> blacklist = groupConfig.getOutboundConfigurationBlacklist();

        if (!isAllowed(pid, whitelist, blacklist)) {
            System.err.println("Configuration PID " + pid + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (clusterConfigurations != null) {
            // update the configurations in the cluster group
            Properties distributedDictionary = clusterConfigurations.get(pid);
            if (distributedDictionary != null) {
                distributedDictionary.remove(key);
                clusterConfigurations.put(pid, distributedDictionary);

                // broadcast the cluster event
                ConfigurationEventTask event = new ConfigurationEventTask();
                event.setSourceGroup(group);
                executionContext.execute(event, group.getNodesExcluding(groupManager.getNode()));
            }
        } else {
            System.out.println("No configuration found in cluster group " + groupName);
        }

        return null;
    }

    /**
     * @return the executionContext
     */
    public DistributedExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @param executionContext the executionContext to set
     */
    public void setExecutionContext(DistributedExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

}
