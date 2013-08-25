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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.osgi.framework.ServiceReference;

/**
 * Listener for the service import.
 */
public class ImportServiceListener implements ListenerHook, Runnable {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ImportServiceListener.class);
    private BundleContext bundleContext;
    private ClusterManager clusterManager;
    private Map<String, EndpointDescription> remoteEndpoints;
    private Set<ListenerInfo> pendingListeners = new LinkedHashSet<ListenerInfo>();
    private final Map<EndpointDescription, ServiceRegistration> registrations = new HashMap<EndpointDescription, ServiceRegistration>();
    private final Map<String, String> producers = new HashMap<String, String>();
    private final Map<String, String> consumers = new HashMap<String, String>();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public void init() {
        remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
        service.scheduleAtFixedRate(this, 0, 5, TimeUnit.SECONDS);
    }

    public void destroy() {
        service.shutdown();
        for (Map.Entry<EndpointDescription, ServiceRegistration> entry : registrations.entrySet()) {
            ServiceRegistration registration = entry.getValue();
            registration.unregister();
        }
        consumers.clear();
        producers.clear();
    }

    @Override
    public void run() {
        for (ListenerInfo listener : pendingListeners) {
            checkListener(listener);
        }
    }

    @Override
    public void added(Collection listeners) {
        for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {

            if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                continue;
            }

            pendingListeners.add(listenerInfo);
            // make sure we only import remote services
            checkListener(listenerInfo);
        }
    }

    @Override
    public void removed(Collection listeners) {
        for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {
            if (listenerInfo.getBundleContext() == bundleContext || listenerInfo.getFilter() == null) {
                continue;
            }

            // make sure we only import remote services
            String filter = "(&" + listenerInfo.getFilter() + "(!(" + Constants.ENDPOINT_FRAMEWORK_UUID + "=" + clusterManager.getMasterCluster().getLocalNode().getId() + ")))";
            // iterate through known services and import them if needed
            Set<EndpointDescription> matches = new LinkedHashSet<EndpointDescription>();
            for (Map.Entry<String, EndpointDescription> entry : remoteEndpoints.entrySet()) {
                EndpointDescription endpointDescription = entry.getValue();
                if (endpointDescription.matches(filter)) {
                    matches.add(endpointDescription);
                }
            }

            for (EndpointDescription endpoint : matches) {
                unImportService(endpoint);
            }

            pendingListeners.remove(listenerInfo);
        }
    }

    /**
     * Check if there is a match for the current {@link ListenerInfo}.
     *
     * @param listenerInfo the listener info.
     */
    private void checkListener(ListenerInfo listenerInfo) {
        // iterate through known services and import them if needed
        Set<EndpointDescription> matches = new LinkedHashSet<EndpointDescription>();
        for (Map.Entry<String, EndpointDescription> entry : remoteEndpoints.entrySet()) {
            EndpointDescription endpointDescription = entry.getValue();
            if (endpointDescription.matches(listenerInfo.getFilter()) && !endpointDescription.getNodes().contains(clusterManager.getMasterCluster().getLocalNode())) {
                matches.add(endpointDescription);
            }
        }

        for (EndpointDescription endpoint : matches) {
            importService(endpoint, listenerInfo);
        }
    }

    /**
     * Import a remote service to the service registry.
     *
     * @param endpoint the endpoint to import.
     * @param listenerInfo the associated listener info.
     */
    private void importService(EndpointDescription endpoint, ListenerInfo listenerInfo) {
        LOGGER.debug("CELLAR DOSGI: importing remote service: " + endpoint.getId());

        String requestProducer = producers.get(endpoint.getId());
        if (requestProducer == null) {
            requestProducer = Constants.INTERFACE_PREFIX + Constants.SEPARATOR + endpoint.getId();
            producers.put(endpoint.getId(), requestProducer);
        }

        String resultConsumer = consumers.get(endpoint.getId());
        if (resultConsumer == null) {
            resultConsumer = Constants.RESULT_PREFIX + Constants.SEPARATOR + clusterManager.getMasterCluster().getLocalNode().getId() + endpoint.getId();
            consumers.put(endpoint.getId(), resultConsumer);
        }

        producers.put(endpoint.getId(), requestProducer);
        consumers.put(endpoint.getId(), resultConsumer);

        ServiceReference<DistributedExecutionContext> sr = null;
        try {
            sr = this.bundleContext.getServiceReference(DistributedExecutionContext.class);
            DistributedExecutionContext executionContext = this.bundleContext.getService(sr);
            RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(endpoint, clusterManager, executionContext);
            ServiceRegistration registration = listenerInfo.getBundleContext().registerService(endpoint.getServiceClass(),
                    remoteServiceFactory,
                    new Hashtable<String, Object>(endpoint.getProperties()));
            registrations.put(endpoint, registration);
            pendingListeners.remove(listenerInfo);
        } finally {
            this.bundleContext.ungetService(sr);
        }
    }

    /**
     * Un-register an imported service.
     *
     * @param endpoint the endpoint to un-register.
     */
    private void unImportService(EndpointDescription endpoint) {
        ServiceRegistration registration = registrations.get(endpoint);
        registration.unregister();

        producers.remove(endpoint.getId());
        consumers.remove(endpoint.getId());
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }
}
