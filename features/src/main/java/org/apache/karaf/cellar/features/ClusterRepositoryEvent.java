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

import java.text.MessageFormat;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.features.RepositoryEvent.EventType;

/**
 * Cluster features repository event.
 */
public class ClusterRepositoryEvent extends Event {

	private EventType type;
    private Boolean install;
    private Boolean uninstall;

    public ClusterRepositoryEvent() {
        super();
    }
    
    public ClusterRepositoryEvent(String id, EventType type) {
        super(id);
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public Boolean getInstall() {
        return install;
    }

    public void setInstall(Boolean install) {
        this.install = install;
    }

    public Boolean getUninstall() {
        return uninstall;
    }

    public void setUninstall(Boolean uninstall) {
        this.uninstall = uninstall;
    }

    @Override
	public String toString() {
		return MessageFormat.format("ClusterRepositoryEvent [type={0}, id={1}, sourceNode={2}, sourceGroup={3}, destination={4}, force={5}, postPublish={6}]", type, id, sourceNode, sourceGroup, destinations, force, postPublish);
	}
    
}
