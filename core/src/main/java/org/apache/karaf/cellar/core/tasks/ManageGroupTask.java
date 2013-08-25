/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core.tasks;

import org.apache.karaf.cellar.core.command.DistributedTask;
import java.util.Set;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.control.ManageGroupActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class ManageGroupTask extends DistributedTask<ManageGroupResultImpl> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ManageGroupTask.class);
    private ManageGroupActions action;
    private String destinationGroup;

    public ManageGroupTask() {
    }

    public ManageGroupTask(ManageGroupActions action, String destinationGroup) {
        this.action = action;
        this.destinationGroup = destinationGroup;
    }

    @Override
    protected ManageGroupResultImpl execute() throws Exception {
        ManageGroupResultImpl result = new ManageGroupResultImpl();
        try {
//        if (Thread.currentThread().isInterrupted()) {
//            return 0;
//        }
            LOGGER.info("Starting execution of the manage group task received from node {}", getSourceNode().getName());

            GroupManager groupManager = super.getService(GroupManager.class);

            if (ManageGroupActions.JOIN.equals(action)) {
                groupManager.joinGroup(destinationGroup);
            } else if (ManageGroupActions.QUIT.equals(action)) {
                groupManager.deregisterNodeFromGroup(destinationGroup);
                if (groupManager.listLocalGroups().isEmpty()) {
                    groupManager.joinGroup(Configurations.DEFAULT_GROUP_NAME);
                }
            } else if (ManageGroupActions.PURGE.equals(action)) {
                groupManager.deregisterNodeFromAllGroups();
                groupManager.joinGroup(Configurations.DEFAULT_GROUP_NAME);
            } else if (ManageGroupActions.SET.equals(action)) {
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
            LOGGER.error("Task wasn't processed for some reason.", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    /**
     * @return the action
     */
    public ManageGroupActions getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ManageGroupActions action) {
        this.action = action;
    }

    /**
     * @return the destinationGroup
     */
    public String getDestinationGroup() {
        return destinationGroup;
    }

    /**
     * @param destinationGroup the destinationGroup to set
     */
    public void setDestinationGroup(String destinationGroup) {
        this.destinationGroup = destinationGroup;
    }
}
