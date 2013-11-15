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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.karaf.cellar.core.event.Event;

/**
 * Handler for cluster remote service call event.
 */
public class RemoteServiceCallTask extends Event<RemoteServiceResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(RemoteServiceCallTask.class);
    private String endpointId;
    private String method;
    private String serviceClass;
    private List<Object> arguments;

    /**
     * Execute a cluster remote service call event.
     *
     * @return
     */
    @Override
    public RemoteServiceResult execute() {

        RemoteServiceResultImpl result = new RemoteServiceResultImpl();
        Object targetService = null;
        try {
            super.getService(this.serviceClass);

            Class[] classes = new Class[0];
            if (this.arguments != null && this.arguments.size() > 0) {
                classes = new Class[this.arguments.size()];
                int i = 0;
                for (Object obj : this.arguments) {
                    classes[i++] = obj.getClass();
                }
            }

            Method method;
            if (classes.length > 0) {
                method = targetService.getClass().getMethod(this.method, classes);
            } else {
                method = targetService.getClass().getMethod(this.method);
            }

            Object obj = method.invoke(targetService, this.arguments.toArray());
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

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }
}
