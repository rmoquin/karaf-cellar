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
package org.apache.karaf.cellar.shell.group;

import java.io.IOException;
import java.util.Map;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@Command(scope = "cluster", name = "group-delete", description = "Delete a cluster group")
public class GroupDeleteCommand extends GroupSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    private ConfigurationAdmin configurationAdmin;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // check if the group doesn't contain nodes
        if (group.getNodes() != null && !group.getNodes().isEmpty()) {
            System.err.println("Cluster group " + groupName + " is not empty");
            return null;
        }

        if (!groupName.equals(Configurations.DEFAULT_GROUP_NAME)) {
            Map<String, Group> groupMap = groupManager.listGroups();
            groupMap.remove(groupName);
            try {
                String groupPid = groupManager.getPidForGroup(groupName);
                Configuration configuration = configurationAdmin.getConfiguration(groupPid, "?");
                configuration.delete();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    /**
     * @return the configurationAdmin
     */
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    /**
     * @param configurationAdmin the configurationAdmin to set
     */
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
