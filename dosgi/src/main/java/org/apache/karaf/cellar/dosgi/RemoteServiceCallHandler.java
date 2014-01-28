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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.exception.CommandExecutionException;

/**
 * Handler for cluster remote service call event.
 */
public class RemoteServiceCallHandler extends CommandHandler<RemoteServiceCall, RemoteServiceResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.dosgi.switch";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RemoteServiceCallHandler.class);

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private BundleContext bundleContext;

    /**
     * Handle a cluster remote service call event.
     *
     * @param event the cluster event to handle.
     * @return
     */
    @Override
    public RemoteServiceResult execute(RemoteServiceCall event) {
        RemoteServiceResult result = new RemoteServiceResult(event.getId());
        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.info("CELLAR DOSGI: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            result.setSuccessful(false);
            result.setThrowable(new CommandExecutionException("CELLAR DOSGI: {} switch is OFF, cluster event is not handled"));
            return result;
        }

        Object targetService = null;
        ServiceReference serviceReference = null;
        try {
            try {
                ServiceReference[] serviceReferences = bundleContext.getServiceReferences(event.getServiceClass(), null);
                if (serviceReferences != null && serviceReferences.length > 0) {
                    serviceReference = serviceReferences[0];
                    targetService = bundleContext.getService(serviceReference);
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.error("CELLAR DOSGI: failed to lookup service", e);
            } finally {
                if (serviceReference != null) {
                    bundleContext.ungetService(serviceReference);
                }
            }

            if (targetService == null) {
                result.setSuccessful(false);
                result.setThrowable(new CommandExecutionException(MessageFormat.format("CELLAR DOSGI: Couldn't find a service for class {0}", event.getServiceClass())));
                return result;
            }
            Class[] classes = new Class[0];
            if (event.getArguments() != null && event.getArguments().size() > 0) {
                classes = new Class[event.getArguments().size()];
                int i = 0;
                for (Object obj : event.getArguments()) {
                    classes[i++] = obj.getClass();
                }
            }

            Method method;
            if (classes.length > 0) {
                method = targetService.getClass().getMethod(event.getMethod(), classes);
            } else {
                method = targetService.getClass().getMethod(event.getMethod());
            }

            Object obj = method.invoke(targetService, event.getArguments().toArray());
            result.setSuccessful(true);
            result.setResult(obj);
        } catch (NoSuchMethodException e) {
            result.setSuccessful(false);
            result.setThrowable(e);
            LOGGER.error("CELLAR DOSGI: unable to find remote method for service", e);
        } catch (InvocationTargetException e) {
            result.setSuccessful(false);
            result.setThrowable(e);
            LOGGER.error("CELLAR DOSGI: unable to invoke remote method for service", e);
        } catch (IllegalAccessException e) {
            result.setSuccessful(false);
            result.setThrowable(e);
            LOGGER.error("CELLAR DOSGI: unable to access remote method for service", e);
        }
        return result;
    }

    /**
     * Get the event type that this handler can handle.
     *
     * @return the remote service call event type.
     */
    @Override
    public Class<RemoteServiceCall> getType() {
        return RemoteServiceCall.class;
    }

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        boolean status = nodeConfiguration.getEnabledEvents().contains(this.getType().getName());
        if (status) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
