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

import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.control.SwitchType;

/**
 *
 * @author rmoquin
 */
class NodeEventConfigurationResultImpl implements NodeEventConfigurationResult {
    private boolean successful = true;
    private SwitchType switchType;
    private SwitchStatus switchStatus;
    private Throwable throwable;

    public NodeEventConfigurationResultImpl() {
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * @param successful the successful to set
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * @return the switchType
     */
    @Override
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
    @Override
    public SwitchStatus getSwitchStatus() {
        return switchStatus;
    }

    /**
     * @param switchStatus the switchStatus to set
     */
    public void setSwitchStatus(SwitchStatus switchStatus) {
        this.switchStatus = switchStatus;
    }

    /**
     * @return the throwable
     */
    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * @param throwable the throwable to set
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
