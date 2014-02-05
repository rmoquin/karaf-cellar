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
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.command.Command;
import org.apache.karaf.features.RepositoryEvent.EventType;

/**
 * Cluster features repository event.
 */
public class ClusterRepositoryEvent extends Command<RespositoryEventResponse> {

    private EventType type;
    private boolean install;
    private boolean uninstall;

    public ClusterRepositoryEvent() {
    }

    public ClusterRepositoryEvent(String id, EventType type) {
        super(id);
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public boolean getInstall() {
        return install;
    }

    public void setInstall(boolean install) {
        this.install = install;
    }

    public boolean getUninstall() {
        return uninstall;
    }

    public void setUninstall(boolean uninstall) {
        this.uninstall = uninstall;
    }

    @Override
    public String toString() {
        return "ClusterRepositoryEvent [type=" + type + ", id=" + id
                + ", sourceNode=" + sourceNode + ", sourceGroup=" + sourceGroup
                + ", destination=" + destination + ", force=" + force
                + ", postPublish=" + postPublish + "]";
    }

    /**
     * @param type the type to set
     */
    public void setType(EventType type) {
        this.type = type;
    }

}
