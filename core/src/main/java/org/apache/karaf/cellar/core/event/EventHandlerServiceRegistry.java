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
package org.apache.karaf.cellar.core.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.command.DistributedResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler service registry.
 */
public class EventHandlerServiceRegistry<E extends Event, R extends DistributedResult> implements EventHandlerRegistry<E, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventHandlerServiceRegistry.class);
    private final Map<String, EventHandler> eventHandlerMap = new ConcurrentHashMap<String, EventHandler>();

    /**
     * Return the appropriate cluster {@code EventHandler} found inside the cluster {@code HandlerRegistry}.
     *
     * @param event the cluster event to handle.
     * @return the corresponding cluster event handler to use.
     */
    @Override
    public EventHandler<E, R> getHandler(E event) {
        if (event != null) {
            Class clazz = event.getClass();
            return eventHandlerMap.get(clazz.getCanonicalName());
        } else {
            LOGGER.warn("Couldn't lookup handler for event: {}", eventHandlerMap);
        }
        return null;
    }

    public void bind(EventHandler handler) {
        if (handler != null && handler.getType() != null) {
            eventHandlerMap.put(handler.getType().getCanonicalName(), handler);
        }
    }

    public void unbind(EventHandler handler) {
        if (handler != null && handler.getType() != null) {
            eventHandlerMap.remove(handler.getType().getCanonicalName());
        }
    }

}
