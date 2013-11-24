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

import org.apache.karaf.cellar.core.command.Result;

/**
 *
 * @author rmoquin
 */
public class NodeEventConfigurationResult extends Result {

    private SwitchType switchType;
    private SwitchStatus switchStatus;

    public NodeEventConfigurationResult() {
    }

    public NodeEventConfigurationResult(String id) {
        super(id);
    }

    /**
     * @return the switchType
     */
    public SwitchType getSwitchType() {
        return switchType;
    }

    /**
     * @param switchType the switchType to set
     */
    public void setSwitchType(SwitchType switchType) {
        this.switchType = switchType;
    }

    /**
     * @return the switchStatus
     */
    public SwitchStatus getSwitchStatus() {
        return switchStatus;
    }

    /**
     * @param switchStatus the switchStatus to set
     */
    public void setSwitchStatus(SwitchStatus switchStatus) {
        this.switchStatus = switchStatus;
    }
}
