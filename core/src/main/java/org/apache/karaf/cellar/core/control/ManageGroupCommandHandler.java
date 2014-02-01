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

import org.apache.karaf.cellar.core.Group;
import java.util.Set;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager group command handler.
 */
public class ManageGroupCommandHandler extends CommandHandler<ManageGroupCommand, ManageGroupResult> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ManageGroupCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.managegroup.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public ManageGroupResult execute(ManageGroupCommand command) {
        ManageGroupResult result = new ManageGroupResult();
        ManageGroupAction action = command.getAction();
        String destinationGroup = command.getDestinationGroup();

        try {
            if (ManageGroupAction.JOIN.equals(action)) {
                groupManager.joinGroup(destinationGroup);
            } else if (ManageGroupAction.QUIT.equals(action)) {
                groupManager.deregisterNodeFromGroup(destinationGroup);
            } else if (ManageGroupAction.PURGE.equals(action)) {
                groupManager.deregisterNodeFromAllGroups();
            } else if (ManageGroupAction.SET.equals(action)) {
                Group localGroup = groupManager.listLocalGroups().iterator().next();
                groupManager.deregisterNodeFromGroup(localGroup.getName());
                groupManager.joinGroup(destinationGroup);
            }
            Set<Group> groups = groupManager.listAllGroups();
            for (Group g : groups) {
                if (g.getName() != null && !g.getName().isEmpty()) {
                    result.getGroups().add(g);
                }
            }
            result.setSuccessful(true);
        } catch (Exception ex) {
            LOGGER.error("Command wasn't processed for some reason.", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    @Override
    public Class<ManageGroupCommand> getType() {
        return ManageGroupCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

}
