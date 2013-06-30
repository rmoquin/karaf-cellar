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

import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.CommandHandler;

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;

/**
 * Manager cluster command handler.
 */
public class ManageGroupCommandHandler extends CommandHandler<ManageGroupCommand, ManageGroupResult> {
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.managegroup.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private CellarCluster masterCluster;
    private GroupManager groupManager;
    private Node node;
    
    public void init() {
        this.node = this.masterCluster.getLocalNode();
    }
    
    @Override
    public ManageGroupResult execute(ManageGroupCommand command) {

        ManageGroupResult result = new ManageGroupResult(command.getId());
        ManageGroupAction action = command.getAction();

        String targetGroupName = command.getGroupName();

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

        addGroupListToResult(result);

        return result;
    }

    /**
     * Add the {@link Cluster} list to the result.
     *
     * @param result the result where to add the cluster list.
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
     * Add {@link Node} to the target {@link CellarCluster}.
     *
     * @param targetClusterName the target cluster name where to add the node.
     */
    public void joinGroup(String targetGroupName) {
        Map<String, Group> groups = groupManager.listGroups();
        if (groups != null && !groups.isEmpty()) {
            Group targetGroup = groups.get(targetGroupName);
            if (targetGroup == null) {
                groupManager.registerGroup(targetGroupName);
            } else if (!targetGroup.getNodes().contains(node)) {
                targetGroup.getNodes().add(node);
                groupManager.listGroups().put(targetGroupName, targetGroup);
                groupManager.registerGroup(targetGroup);
            }
        }
    }

    /**
     * Leave the target {@link CellarCluster}.
     *
     * @param targetClusterName the target cluster to leave.
     */
    public void quitGroup(String targetGroupName) {
        Map<String, Group> groups = groupManager.listGroups();
        if (groups != null && !groups.isEmpty()) {
            Group targetGroup = groups.get(targetGroupName);
            if (targetGroup.getNodes().contains(node)) {
                targetGroup.getNodes().remove(node);
                groupManager.unRegisterGroup(targetGroup);
            }
        }
    }

    /**
     * Remove {@link Node} from all {@link CellarCluster}s.
     */
    public void purgeGroups() {
        Set<String> groupNames = groupManager.listGroupNames(node);
        if (groupNames != null && !groupNames.isEmpty()) {
            for (String targetGroupName : groupNames) {
                quitGroup(targetGroupName);
            }
        }
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
}
