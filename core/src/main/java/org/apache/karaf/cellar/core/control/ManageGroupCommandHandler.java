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
package org.apache.karaf.cellar.core.control;

import java.util.Set;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.CommandHandler;

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager cluster command handler.
 */
public class ManageGroupCommandHandler extends CommandHandler<ManageGroupCommand, ManageGroupResult> {
    private static Logger LOGGER = LoggerFactory.getLogger(ManageGroupCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.managegroup.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private CellarCluster masterCluster;
    private GroupManager groupManager;
    private ConfigurationAdmin configurationAdmin;

    public void init() {
    }

    @Override
    public ManageGroupResult execute(ManageGroupCommand command) {

        ManageGroupResult result = new ManageGroupResult(command.getId());
        ManageGroupAction action = command.getAction();

        String targetGroupName = command.getGroupName();
        try {
            if (ManageGroupAction.JOIN.equals(action)) {
                joinGroup(targetGroupName);
            } else if (ManageGroupAction.QUIT.equals(action)) {
                quitGroup(targetGroupName);
                if (groupManager.listLocalGroups().isEmpty()) {
                    joinGroup(Configurations.DEFAULT_GROUP_NAME);
                }
            } else if (ManageGroupAction.PURGE.equals(action)) {
                purgeGroups();
                joinGroup(Configurations.DEFAULT_GROUP_NAME);
            } else if (ManageGroupAction.SET.equals(action)) {
                Group localGroup = groupManager.listLocalGroups().iterator().next();
                quitGroup(localGroup.getName());
                joinGroup(targetGroupName);
            }
        } catch (Exception ex) {
            LOGGER.error("Error handling manage group command.", ex);
        }
        addGroupListToResult(result);

        return result;
    }

    /**
     * Add the {@link Group} list to the result.
     *
     * @param result the result where to add the group list.
     */
    public void addGroupListToResult(ManageGroupResult result) {
        Set<Group> groups = groupManager.listAllGroups();

        for (Group g : groups) {
            if (g.getName() != null && !g.getName().isEmpty()) {
                result.getGroups().add(g);
            }
        }
    }

    /**
     * Add {@link Node} to the target {@link Group}.
     *
     * @param targetGroupName the name of the group to join.
     */
    public void joinGroup(String targetGroupName) throws ConfigurationException {
        groupManager.joinGroup(targetGroupName);
    }

    /**
     * Leave the target {@link CellarCluster}.
     *
     * @param targetGroupName the target group to leave.
     */
    public void quitGroup(String targetGroupName) {
        groupManager.deregisterNodeFromGroup(targetGroupName);
    }

    /**
     * Remove {@link Node} from all {@link Group}s.
     */
    public void purgeGroups() {
        this.groupManager.deregisterNode();;
    }

    @Override
    public Class<ManageGroupCommand> getType() {
        return ManageGroupCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
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
