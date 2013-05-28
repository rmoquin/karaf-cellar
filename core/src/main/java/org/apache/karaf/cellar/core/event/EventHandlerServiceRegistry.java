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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler service registry.
 */
public class EventHandlerServiceRegistry<E extends Event> implements EventHandlerRegistry<E> {
    private static transient final Logger LOGGER = LoggerFactory.getLogger(EventHandlerServiceRegistry.class);
    private Map<Class, EventHandler> eventHandlerMap = new ConcurrentHashMap<Class, EventHandler>();

    /**
     * Return the appropriate cluster {@code EventHandler} found inside the cluster {@code HandlerRegistry}.
     *
     * @param event the cluster event to handle.
     * @return the corresponding cluster event handler to use.
     */
    @Override
    public EventHandler<E> getHandler(E event) {
        if (event != null) {
            Class clazz = event.getClass();
            LOGGER.info("Getting eventhandler for event class: " + clazz.toString());
            return eventHandlerMap.get(clazz);
        }
        LOGGER.info("Not event handler could be retrieved for event: " + event);
        return null;
    }

    public void bind(EventHandler handler) {
        if (handler != null && handler.getType() != null) {
            LOGGER.info("Binding event handler type: " + handler.getType());
            eventHandlerMap.put(handler.getType(), handler);
        }
    }

    public void unbind(EventHandler handler) {
        if (handler != null && handler.getType() != null) {
            LOGGER.info("Unbinding event handler type: " + handler.getType());
            eventHandlerMap.remove(handler.getType());
        }
    }
}
