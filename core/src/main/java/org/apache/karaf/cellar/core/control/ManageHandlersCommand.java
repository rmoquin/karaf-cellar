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
 * Manage handlers command.
 */
public class ManageHandlersCommand extends Command<ManageHandlersResult> {

    private String handlerName;
    private Boolean status = true;

    public ManageHandlersCommand() {
    }

    public ManageHandlersCommand(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return super.toString() + "ManageHandlersCommand{" + "handlerName=" + handlerName + ", status=" + status + '}';
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

}
