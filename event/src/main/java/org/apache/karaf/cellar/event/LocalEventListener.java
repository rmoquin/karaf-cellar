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
package org.apache.karaf.cellar.event;

import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import org.apache.karaf.cellar.core.CellarCluster;

public class LocalEventListener extends EventSupport implements EventHandler {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalEventListener.class);

    @Override
    public void handleEvent(Event event) {
        // ignore log entry event
        if (event.getTopic().startsWith("org/osgi/service/log/LogEntry")) {
            return;
        }

        try {
            if (event.getTopic() != null) {
                Collection<CellarCluster> clusters = null;
                try {
                    clusters = clusterManager.getLocalClusters();
                } catch (Exception e) {
                    LOGGER.warn("Failed to list local groups. Is Cellar uninstalling ?", e);
                    return;
                }

                // filter already processed events
                if (hasEventProperty(event, Constants.EVENT_PROCESSED_KEY)) {
                    if (event.getProperty(Constants.EVENT_PROCESSED_KEY).equals(Constants.EVENT_PROCESSED_VALUE)) {
                        LOGGER.debug("CELLAR EVENT: filtered out event {}", event.getTopic());
                        return;
                    }
                }

                if (clusters != null && !clusters.isEmpty()) {
                    for (CellarCluster cluster : clusters) {
                        // check if the producer is ON
                        if (cluster.emitsEvents()) {
                            LOGGER.warn("CELLAR EVENT: cluster event producer is OFF");
                            return;
                        }
                        String topicName = event.getTopic();
                        Map<String, Object> properties = getEventProperties(event);
                        if (isAllowed(cluster.getName(), Constants.CATEGORY, topicName, EventType.OUTBOUND)) {
                            // broadcast the event
                            ClusterEvent clusterEvent = new ClusterEvent(topicName, properties);
                            clusterEvent.setSourceCluster(cluster);
                            cluster.produce(clusterEvent);
                        } else {
                            LOGGER.warn("CELLAR EVENT: event {} is marked as BLOCKED OUTBOUND", topicName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR EVENT: failed to handle event", e);
        }
    }

    /**
     * Initialization method.
     */
    public void init() {
    }

    /**
     * Destruction method.
     */
    public void destroy() {
    }
}
