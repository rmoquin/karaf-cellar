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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for remote service.
 */
public class RemoteServiceFactory implements ServiceFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RemoteServiceFactory.class);

    private final EndpointDescription description;
    private final ClusterManager clusterManager;
    private final DistributedExecutionContext executionContext;

    /**
     *
     * @param description
     * @param clusterManager
     * @param executionContext
     */
    public RemoteServiceFactory(EndpointDescription description, ClusterManager clusterManager, DistributedExecutionContext executionContext) {
        this.description = description;
        this.clusterManager = clusterManager;
        this.executionContext = executionContext;
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        ClassLoader classLoader = new RemoteServiceProxyClassLoader(bundle);
        List<Class> interfaces = new ArrayList<Class>();
        String interfaceName = description.getServiceClass();
        try {
            interfaces.add(classLoader.loadClass(interfaceName));
        } catch (ClassNotFoundException e) {
            LOGGER.error("Couldn't find service class.", e);
        }
        RemoteServiceInvocationHandler handler = new RemoteServiceInvocationHandler(description, interfaceName, clusterManager, executionContext);
        return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class[interfaces.size()]), handler);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        // nothing to do
    }

}
