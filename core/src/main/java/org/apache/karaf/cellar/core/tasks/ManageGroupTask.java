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

import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author rmoquin
 */
public class ManageGroupTask implements Callable<GroupTaskResult> {
    private ManageGroupAction action;
    private String groupName;
    private Node sourceNode;
    protected Group sourceGroup;
    
    public ManageGroupTask() {
    }

    public ManageGroupTask(ManageGroupAction action, String groupName) {
        this.action = action;
        this.groupName = groupName;
    }

    @Override
    public GroupTaskResult call() throws Exception {
//        if (Thread.currentThread().isInterrupted()) {
//            return 0;
//        }
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        BundleContext bundleContext = bundle.getBundleContext();
        ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences(GroupManager.class.getCanonicalName(), null);
        if (serviceReferences != null && serviceReferences.length > 0) {
            ServiceReference ref = serviceReferences[0];
            GroupManager groupManager = (GroupManager) bundleContext.getService(ref);
            try {
                GroupTaskResult result = new GroupTaskResult();

                String targetGroupName = this.groupName;
                if (ManageGroupAction.JOIN.equals(action)) {
                    groupManager.joinGroup(targetGroupName);
                } else if (ManageGroupAction.QUIT.equals(action)) {
                    groupManager.deregisterNodeFromGroup(targetGroupName);
                    if (groupManager.listLocalGroups().isEmpty()) {
                        groupManager.joinGroup(Configurations.DEFAULT_GROUP_NAME);
                    }
                } else if (ManageGroupAction.PURGE.equals(action)) {
                    groupManager.deregisterNodeFromAllGroups();
                    groupManager.joinGroup(Configurations.DEFAULT_GROUP_NAME);
                } else if (ManageGroupAction.SET.equals(action)) {
                    Group localGroup = groupManager.listLocalGroups().iterator().next();
                    groupManager.deregisterNodeFromGroup(localGroup.getName());
                    groupManager.joinGroup(targetGroupName);
                }
                Set<Group> groups = groupManager.listAllGroups();
                for (Group g : groups) {
                    if (g.getName() != null && !g.getName().isEmpty()) {
                        result.getGroups().add(g);
                    }
                }
                return result;
            } finally {
                bundleContext.ungetService(ref);
            }
        }
        throw new Exception("Task wasn't processed for some reason: " + this.toString());
    }

    @Override
    public String toString() {
        return String.format("ManageGroupTask{action=%s, groupName=%s, sourceNode=%s, sourceGroup=%s%s", action, groupName, sourceNode, sourceGroup, '}');
    }

    /**
     * @return the action
     */
    public ManageGroupAction getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(ManageGroupAction action) {
        this.action = action;
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @param groupName the groupName to set
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Node getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }

    public Group getSourceGroup() {
        return sourceGroup;
    }

    public void setSourceGroup(Group sourceGroup) {
        this.sourceGroup = sourceGroup;
    }
}
