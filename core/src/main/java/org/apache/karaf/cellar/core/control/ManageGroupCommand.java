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

}
