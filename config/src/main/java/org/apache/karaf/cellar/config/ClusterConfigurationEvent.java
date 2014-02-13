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
 *//*
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
package org.apache.karaf.cellar.config;

import org.apache.karaf.cellar.config.shell.ConfigurationAction;
import org.apache.karaf.cellar.core.command.Command;

/**
 * Cluster configuration event.
 */
public class ClusterConfigurationEvent extends Command<ConfigurationTaskResult> {

    private ConfigurationAction type;
    private String propertyName;
    private Object propertyValue;

    public ClusterConfigurationEvent() {
    }

    public ClusterConfigurationEvent(String id) {
        super(id);
    }

    public ConfigurationAction getType() {
        return type;
    }

    public void setType(ConfigurationAction type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ClusterConfigurationEvent [type=" + type + ", id=" + id
                + ", sourceNode=" + sourceNode + ", sourceGroup=" + sourceGroup
                + ", destination=" + destination + ", force=" + force
                + ", postPublish=" + postPublish + "]";
    }

    /**
     * @return the propertyName
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return the propertyValue
     */
    public Object getPropertyValue() {
        return propertyValue;
    }

    /**
     * @param propertyValue the propertyValue to set
     */
    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }

}
