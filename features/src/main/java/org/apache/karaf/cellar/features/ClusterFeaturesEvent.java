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
import org.apache.karaf.features.FeatureEvent.EventType;

/**
 * Cluster features event.
 */
public class ClusterFeaturesEvent extends Command<FeatureEventResponse> {

    private static final String separator = "/";

    private String name;
    private String version;
    private Boolean noClean;
    private Boolean noRefresh;
    private EventType type;

    public ClusterFeaturesEvent() {
    }

    public ClusterFeaturesEvent(String name, String version, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noClean = false;
        this.noRefresh = false;
        this.type = type;
    }

    public ClusterFeaturesEvent(String name, String version, Boolean noClean, Boolean noRefresh, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noClean = noClean;
        this.noRefresh = noRefresh;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ClusterFeaturesEvent{" + "name=" + name + ", version=" + version + ", noClean=" + noClean + ", noRefresh=" + noRefresh + ", type=" + type + '}';
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Boolean getNoClean() {
        return noClean;
    }

    public Boolean getNoRefresh() {
        return noRefresh;
    }

    public EventType getType() {
        return type;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @param noClean the noClean to set
     */
    public void setNoClean(Boolean noClean) {
        this.noClean = noClean;
    }

    /**
     * @param noRefresh the noRefresh to set
     */
    public void setNoRefresh(Boolean noRefresh) {
        this.noRefresh = noRefresh;
    }

    /**
     * @param type the type to set
     */
    public void setType(EventType type) {
        this.type = type;
    }

}
