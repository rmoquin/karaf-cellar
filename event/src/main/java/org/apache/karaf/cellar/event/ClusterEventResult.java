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
package org.apache.karaf.cellar.event;

import java.io.Serializable;
import java.util.Map;
import org.apache.karaf.cellar.core.command.Result;

/**
 *
 * @author rmoquin
 */
public class ClusterEventResult extends Result {

    private String topicName;
    private Map<String, Serializable> properties;

    public ClusterEventResult() {
    }

    public ClusterEventResult(String id) {
        super(id);
    }

    public ClusterEventResult(String topicName, Map<String, Serializable> properties) {
        super(topicName);
        this.topicName = topicName;
        this.properties = properties;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Map<String, Serializable> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }
}
