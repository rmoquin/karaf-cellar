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
package org.apache.karaf.cellar.core.control;

import org.apache.karaf.cellar.core.command.Command;

/**
 *
 * @author rmoquin
 */
public class NodeConfigurationCommand extends Command<NodeConfigurationResult> {

    private SwitchStatus status = null;
    private SwitchType type = null;

    public NodeConfigurationCommand() {
    }

    public NodeConfigurationCommand(String id) {
        super(id);
    }

    public NodeConfigurationCommand(String id, SwitchStatus status, SwitchType type) {
        super(id);
        this.status = status;
        this.type = type;
    }

    /**
     * @return the status
     */
    public SwitchStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(SwitchStatus status) {
        this.status = status;
    }

    /**
     * @return the type
     */
    public SwitchType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SwitchType type) {
        this.type = type;
    }
}
