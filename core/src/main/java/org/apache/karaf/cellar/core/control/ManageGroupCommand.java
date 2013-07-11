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

import org.apache.karaf.cellar.core.command.Command;

/**
 * Manager group command.
 */
public class ManageGroupCommand extends Command<ManageGroupResult> {
    private ManageGroupAction action;
    private String groupName;

    public ManageGroupCommand() {
    }

    public ManageGroupCommand(String id) {
        super(id);
    }

    public ManageGroupAction getAction() {
        return action;
    }

    public void setAction(ManageGroupAction action) {
        this.action = action;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.action != null ? this.action.hashCode() : 0);
        hash = 97 * hash + (this.groupName != null ? this.groupName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ManageGroupCommand other = (ManageGroupCommand) obj;
        if (this.action != other.action) {
            return false;
        }
        if ((this.groupName == null) ? (other.groupName != null) : !this.groupName.equals(other.groupName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ManageGroupCommand{" + "action=" + action + ", groupName=" + groupName + '}';
    }
}
