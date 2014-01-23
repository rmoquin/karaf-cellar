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
package org.apache.karaf.cellar.core.command;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventHandlerRegistry;
import org.apache.karaf.shell.console.BundleContextAware;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author rmoquin
 */
public class DistributedTask<T extends DistributedResult> implements Callable<T>, Serializable {

    protected transient BundleContext bundleContext;
    protected List<ServiceReference<?>> usedReferences;
    private Event event;

    public DistributedTask() {
    }

    public DistributedTask(Event event) {
        this.event = event;
    }

    @Override
    public T call() throws Exception {
        try {
            bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            EventHandlerRegistry handlerRegistry = this.getService(EventHandlerRegistry.class);
            EventHandler<Event, T> handler = handlerRegistry.getHandler(event);
            if (handler != null) {
                return handler.execute(event);
            } else {
                throw new ServiceException(MessageFormat.format("The required command handler could not be looked up in the command registry for event type {}", event));
            }
        } finally {
            ungetServices();
        }
    }

    protected <T> T getService(String clazz) {
        ServiceReference<T> sr = (ServiceReference<T>) bundleContext.getServiceReference(clazz);
        if (sr != null) {
            return getService(sr);
        } else {
            return null;
        }
    }

    protected <T> T getService(Class<T> clazz) {
        ServiceReference<T> sr = bundleContext.getServiceReference(clazz);
        if (sr != null) {
            return getService(sr);
        } else {
            return null;
        }
    }

    protected <T> T getService(ServiceReference<T> reference) {
        T t = bundleContext.getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference<?>>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference<?> ref : usedReferences) {
                bundleContext.ungetService(ref);
            }
        }
    }

    /**
     * @return the event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(Event event) {
        this.event = event;
    }
}
