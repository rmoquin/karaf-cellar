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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.Command;

/**
 *
 * @author rmoquin
 */
public class NodeEventConfigurationCommand extends Command<NodeEventConfigurationResult> {

    private SwitchStatus status = null;
    private SwitchType type = null;

    public NodeEventConfigurationCommand() {
    }

    public NodeEventConfigurationCommand(String id) {
        super(id);
    }

    public NodeEventConfigurationCommand(SwitchStatus status, SwitchType type) {
        this.status = status;
        this.type = type;
    }

    @Override
    public Node getSourceNode() {
        return getSourceNode();
    }

    @Override
    public void setSourceNode(Node sourceNode) {
        this.setSourceNode(sourceNode);
    }

    @Override
    public Group getSourceGroup() {
        return getSourceGroup();
    }

    @Override
    public void setSourceGroup(Group sourceGroup) {
        this.setSourceGroup(sourceGroup);
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
