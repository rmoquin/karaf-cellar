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
    private String destinationGroup;

    public ManageGroupCommand() {
    }

    public ManageGroupCommand(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return super.toString() + "ManageGroupCommand{" + "action=" + action + ", destinationGroup=" + destinationGroup + '}';
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
