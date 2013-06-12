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
package org.apache.karaf.cellar.hazelcast;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.karaf.cellar.core.ClusterManager;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.SynchronizationConfiguration;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster manager implementation powered by Hazelcast.
 */
public class HazelcastClusterManager implements ClusterManager {
    private static transient Logger LOGGER = LoggerFactory.getLogger(HazelcastClusterManager.class);
    private String clusterPid;
    private List<CellarCluster> clusters;
    private Map<String, CellarCluster> clustersByName = new ConcurrentHashMap<String, CellarCluster>();
    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> clusterConsumer = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private EventTransportFactory eventTransportFactory;
    private ConfigurationAdmin configurationAdmin;
    private SynchronizationConfiguration synchronizationConfig;

    public void init() {
    }

    public void destroy() {
    }
    
    public void bind(CellarCluster cluster) {
        LOGGER.info("Cellar cluster with name: " + cluster.getName() + " in now bound as a service.");
        String clusterName = cluster.getName();
        Properties serviceProperties = new Properties();
            serviceProperties.put("type", "cluster");
            serviceProperties.put("name", clusterName);
        if (!producerRegistrations.containsKey(clusterName)) {
            EventProducer producer = groupProducers.get(clusterName);
            if (producer == null) {
                producer = eventTransportFactory.getEventProducer(cluster, Boolean.TRUE);
                groupProducers.put(clusterName, producer);
            }
            ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, (Dictionary) serviceProperties);
            producerRegistrations.put(clusterName, producerRegistration);
        }

        if (!consumerRegistrations.containsKey(clusterName)) {
            EventConsumer consumer = clusterConsumer.get(clusterName);
            if (consumer == null) {
                consumer = eventTransportFactory.getEventConsumer(cluster, true);
                clusterConsumer.put(clusterName, consumer);
            } else if (!consumer.isConsuming()) {
                consumer.start();
            }
            ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, (Dictionary) serviceProperties);
            consumerRegistrations.put(clusterName, consumerRegistration);
        }
    }

    public void unbind(CellarCluster cluster) {
        LOGGER.info("Cellar cluster with name: " + cluster.getName() + " in now unbound as a service.");
        String clusterName = cluster.getName();
        clustersByName.remove(clusterName);
        // un-register cluster group consumers
        if (consumerRegistrations != null && !consumerRegistrations.isEmpty()) {
            ServiceRegistration consumerRegistration = consumerRegistrations.get(clusterName);
            if (consumerRegistration != null) {
                consumerRegistration.unregister();
                consumerRegistrations.remove(clusterName);
            }
        }

        // un-register cluster group producers
        if (producerRegistrations != null && !producerRegistrations.isEmpty()) {
            ServiceRegistration producerRegistration = producerRegistrations.get(clusterName);
            if (producerRegistration != null) {
                producerRegistration.unregister();
                producerRegistrations.remove(clusterName);
            }
        }

        // remove consumers & producers
        groupProducers.remove(clusterName);
        EventConsumer consumer = clusterConsumer.remove(clusterName);
        if (consumer != null) {
            consumer.stop();
        }
    }
    
    @Override
    public Set<Node> listNodesAllClusters() {
        Set<Node> nodes = new HashSet<Node>();
        for (CellarCluster cellarCluster : this.clusters) {
            nodes.addAll(cellarCluster.listNodes());
        }
        return nodes;
    }

    /**
     * @return the clusters
     */
    @Override
    public List<CellarCluster> getClusters() {
        return clusters;
    }
    
    @Override
    public CellarCluster getFirstCluster() {
        return clusters.iterator().next();
    }
    
    @Override
    public String generateId() {
        if(!this.clusters.isEmpty()) {
            CellarCluster cluster = this.clusters.iterator().next();
            return cluster.generateId();
        } else {
            throw new IllegalStateException("Not participating in any clusters so a unique id cannot be generated.");
        }
    }

    @Override
    public void joinCluster(String clusterName) {
        this.createCluster(clusterName);
    }

    @Override
    public void leaveCluster(String clusterName) {
        CellarCluster cluster = this.findClusterByName(clusterName);
        //TODO figure this out.
    }
    
    private void createCluster(String clusterName) {
        Configuration configuration;
        try {
            configuration = this.configurationAdmin.getConfiguration(clusterPid, "?");
            configuration.getProperties().put("name", clusterName);
            configuration.update();
        } catch (IOException ex) {
            LOGGER.error("Error saving configuration " + clusterPid, ex);
        }
    }

    @Override
    public CellarCluster findClusterByName(String clusterName) {
        return this.clustersByName.get(clusterName);
    }

    @Override
    public Node findNodeById(String nodeId) {
        for (CellarCluster cellarCluster : clusters) {
            Node node = cellarCluster.findNodeById(nodeId);
            if(node != null)
                return node;
        }
        return null;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    /**
     * @return the clusterPid
     */
    public String getClusterPid() {
        return clusterPid;
    }

    /**
     * @param clusterPid the clusterPid to set
     */
    public void setClusterPid(String clusterPid) {
        this.clusterPid = clusterPid;
    }

    /**
     * @param clusters the clusters to set
     */
    public void setClusters(List<CellarCluster> clusters) {
        this.clusters = clusters;
    }

    /**
     * @return the configurationAdmin
     */
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    /**
     * @param configurationAdmin the configurationAdmin to set
     */
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    /**
     * @return the synchronizationConfig
     */
    public SynchronizationConfiguration getSynchronizationConfig() {
        return synchronizationConfig;
    }

    /**
     * @param synchronizationConfig the synchronizationConfig to set
     */
    public void setSynchronizationConfig(SynchronizationConfiguration synchronizationConfig) {
        this.synchronizationConfig = synchronizationConfig;
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
