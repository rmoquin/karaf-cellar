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

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
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
    private String hazelcastConfig;
    private String clusterPid;
    private BundleClassLoader bundleClassLoader;
    private CellarCluster mainCluster;
    private List<String> clusterNames = new ArrayList<String>();
    private Map<String, CellarCluster> clustersByName = new ConcurrentHashMap<String, CellarCluster>();
    private transient List<? extends Synchronizer> synchronizers;
    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> clusterConsumer = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private EventTransportFactory eventTransportFactory;
    private ConfigurationAdmin configurationAdmin;
    private SynchronizationConfiguration synchronizationConfig;
    private boolean producer = true;
    private boolean consumer = true;
    private boolean enableBundleEvents = true;
    private boolean enableConfigurationEvents = true;
    private boolean enableFeatureEvents = true;
    private boolean enableDOSGIEvents = true;
    private boolean enableClusterEvents = true;
    private boolean enableOBRBundleEvents = true;
    private boolean enableObrEvents = true;

    public void init() {
    }

    public void destroy() {
    }

    public void updated(Map<String, ?> properties) {
        String clusterName = properties.get("clusterNames").toString();
        if (!clusterNames.contains(clusterName)) {
            clusterNames.add(clusterName);
            try {
                this.createCluster(clusterName);
            } catch (Exception ex) {
                LOGGER.error("Error creating cluster, might be able to ignore it.", ex);
            }
        }
    }

    @Override
    public Set<Node> listNodesAllClusters() {
        Set<Node> nodes = new HashSet<Node>();
        for (CellarCluster cellarCluster : this.clustersByName.values()) {
            nodes.addAll(cellarCluster.listNodes());
        }
        return nodes;
    }

    @Override
    public boolean isLocalCluster(CellarCluster cluster) {
        return this.mainCluster.getLocalNode().equals(cluster.getLocalNode());
    }

    @Override
    public Collection<CellarCluster> getLocalClusters() {
        return this.getClusters();
    }

    @Override
    public String generateId() {
        CellarCluster cluster = this.getMasterCluster();
        return cluster.generateId();
    }

    @Override
    public void joinCluster(String clusterName) {
        this.createCluster(clusterName);
    }

    @Override
    public void leaveCluster(String clusterName) {
        LOGGER.info("Removing Cellar cluster with name: " + clusterName + ".");
        CellarCluster cluster = this.findClusterByName(clusterName);
        if (cluster != null) {
            cluster.shutdown();
        } else {
            LOGGER.warn("No cluster with the name " + clusterName + " existed to shutdown, will still attempt the rest of the cleanup.");
        }
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
        EventConsumer eventConsumer = clusterConsumer.remove(clusterName);
        if (eventConsumer != null) {
            eventConsumer.stop();
        }
        persistConfig(clusterName, false);
    }

    @Override
    public void deleteCluster(String clusterName) {
        if (!clusterName.equals(this.mainCluster.getName())) {
            this.leaveCluster(clusterName);
        }
    }

    @Override
    public void createCluster(String clusterName) {
        if (this.clustersByName.containsKey(clusterName)) {
            throw new IllegalArgumentException("This node is already a member of the cluster named: " + clusterName);
        }
        HazelcastCluster cluster = new HazelcastCluster();
        Config cfg = createNewConfig(clusterName);
        if (mainCluster == null) {
            mainCluster = cluster;
            cluster.init(clusterName, cfg, true);
            cluster.setSynchronizers(synchronizers);
            this.eventTransportFactory.setMasterCluster(mainCluster);
        } else {
            cluster.init(clusterName, cfg, false);
        }
        this.clustersByName.put(clusterName, cluster);

        try {
            Properties serviceProperties = new Properties();
            serviceProperties.put("type", "cluster");
            serviceProperties.put("name", clusterName);
            if (!producerRegistrations.containsKey(clusterName)) {
                EventProducer eventProducer = groupProducers.get(clusterName);
                if (eventProducer == null) {
                    eventProducer = eventTransportFactory.getEventProducer(clusterName, Boolean.TRUE);
                    groupProducers.put(clusterName, eventProducer);
                }
                ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), eventProducer, (Dictionary) serviceProperties);
                producerRegistrations.put(clusterName, producerRegistration);
                cluster.setEventProducer(eventProducer);
            }

            if (!consumerRegistrations.containsKey(clusterName)) {
                EventConsumer eventConsumer = clusterConsumer.get(clusterName);
                if (eventConsumer == null) {
                    eventConsumer = eventTransportFactory.getEventConsumer(clusterName, true);
                    clusterConsumer.put(clusterName, eventConsumer);
                } else if (!eventConsumer.isConsuming()) {
                    eventConsumer.start();
                }
                ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), eventConsumer, (Dictionary) serviceProperties);
                consumerRegistrations.put(clusterName, consumerRegistration);
            }
        } catch (Exception ex) {
            LOGGER.error("Error initializing the cluster manager: ", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public CellarCluster findClusterByName(String clusterName) {
        return this.clustersByName.get(clusterName);
    }

    @Override
    public Node findNodeById(String nodeId) {
        for (CellarCluster cellarCluster : clustersByName.values()) {
            Node node = cellarCluster.findNodeById(nodeId);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Persist the config, the second boolean parameter indicates where to add or remove
     */
    protected void persistConfig(String clusterName, boolean addCluster) {
        try {
            Configuration configuration = this.configurationAdmin.getConfiguration(clusterPid, "?");
            if (addCluster) {
                clusterNames.add(clusterName);
            } else {
                clusterNames.remove(clusterName);
            }
            Dictionary<String, Object> properties = configuration.getProperties();
            properties.put("clusterNames", clusterNames);
            configuration.update(properties);
        } catch (IOException ex) {
            LOGGER.error("Error saving configuration " + clusterPid, ex);
        }
    }

    protected Config createNewConfig(String name) {
        Config cfg = null;
        try {
            cfg = new FileSystemXmlConfig(hazelcastConfig);
        } catch (FileNotFoundException ex) {
            LOGGER.error("The default hazelcast config couldn't be found, hazelcast will try to drop back to it's internal default.", ex);
            cfg = new Config();
        }
        cfg.getGroupConfig().setName(name);
        cfg.setClassLoader(bundleClassLoader);
        GlobalSerializerConfig globalConfig = new GlobalSerializerConfig();
        globalConfig.setClassName("java.lang.Object");
        globalConfig.setImplementation(new GenericCellarSerializer());
        cfg.getSerializationConfig().setGlobalSerializerConfig(globalConfig);
        return cfg;
    }
    
    /**
     * Get a Map from the main administrative cluster.
     *
     * @param mapName the Map name.
     * @return the Map with the specifed name.
     */
    @Override
    public Map getMap(String mapName) {
        return this.mainCluster.getMap(mapName);
    }

    /**
     * Get a List from the main administrative cluster.
     *
     * @param listName the List name.
     * @return the List with the specifed name.
     */
    @Override
    public List getList(String listName) {
        return this.mainCluster.getList(listName);
    }
    
    /**
     * Get a Topic from the main administrative cluster.
     *
     * @param topicName the Topic name.
     * @return the Topic with the specifed name.
     */
    public ITopic getTopic(String topicName) {
        return ((HazelcastClusterManager)this.mainCluster).getTopic(topicName);
    }
    
    /**
     * Get a Queue from the main administrative cluster.
     *
     * @param queueName the Queue name.
     * @return the Queue with the specifed name.
     */
    public IQueue getQueue(String queueName) {
        return ((HazelcastClusterManager)this.mainCluster).getQueue(queueName);
    }

    /**
     * Get a Set from the main administrative cluster.
     *
     * @param setName the Set name.
     * @return the Set with the specifed name.
     */
    @Override
    public Set getSet(String setName) {
        return this.mainCluster.getSet(setName);
    }

    /**
     * @return the clusters
     */
    @Override
    public Collection<CellarCluster> getClusters() {
        return clustersByName.values();
    }

    /**
     * @return the clusters
     */
    public List<String> getClusterNames() {
        return clusterNames;
    }

    public void setClusterNames(List<String> clusterNames) {
        this.clusterNames = clusterNames;
    }

    @Override
    public Map<String, CellarCluster> getClusterMap() {
        return clustersByName;
    }

    @Override
    public CellarCluster getMasterCluster() {
        return this.mainCluster;
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
    public void setClustersMap(Map<String, CellarCluster> clustersByName) {
        this.clustersByName = clustersByName;
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

    /**
     * @return the producer
     */
    @Override
    public boolean isProducer() {
        return producer;
    }

    /**
     * @param producer the producer to set
     */
    public void setProducer(boolean producer) {
        this.producer = producer;
    }

    /**
     * @return the consumer
     */
    @Override
    public boolean isConsumer() {
        return consumer;
    }

    /**
     * @param consumer the consumer to set
     */
    public void setConsumer(boolean consumer) {
        this.consumer = consumer;
    }

    /**
     * @return the enableBundleEvents
     */
    @Override
    public boolean isEnableBundleEvents() {
        return enableBundleEvents;
    }

    /**
     * @param enableBundleEvents the enableBundleEvents to set
     */
    public void setEnableBundleEvents(boolean enableBundleEvents) {
        this.enableBundleEvents = enableBundleEvents;
    }

    /**
     * @return the enableConfigurationEvents
     */
    @Override
    public boolean isEnableConfigurationEvents() {
        return enableConfigurationEvents;
    }

    /**
     * @param enableConfigurationEvents the enableConfigurationEvents to set
     */
    public void setEnableConfigurationEvents(boolean enableConfigurationEvents) {
        this.enableConfigurationEvents = enableConfigurationEvents;
    }

    /**
     * @return the enableFeatureEvents
     */
    @Override
    public boolean isEnableFeatureEvents() {
        return enableFeatureEvents;
    }

    /**
     * @param enableFeatureEvents the enableFeatureEvents to set
     */
    public void setEnableFeatureEvents(boolean enableFeatureEvents) {
        this.enableFeatureEvents = enableFeatureEvents;
    }

    /**
     * @return the enableDOSGIEvents
     */
    @Override
    public boolean isEnableDOSGIEvents() {
        return enableDOSGIEvents;
    }

    /**
     * @param enableDOSGIEvents the enableDOSGIEvents to set
     */
    public void setEnableDOSGIEvents(boolean enableDOSGIEvents) {
        this.enableDOSGIEvents = enableDOSGIEvents;
    }

    /**
     * @return the enableClusterEvents
     */
    @Override
    public boolean isEnableClusterEvents() {
        return enableClusterEvents;
    }

    /**
     * @param enableClusterEvents the enableClusterEvents to set
     */
    public void setEnableClusterEvents(boolean enableClusterEvents) {
        this.enableClusterEvents = enableClusterEvents;
    }

    /**
     * @return the enableOBRBundleEvents
     */
    @Override
    public boolean isEnableOBRBundleEvents() {
        return enableOBRBundleEvents;
    }

    /**
     * @param enableOBRBundleEvents the enableOBRBundleEvents to set
     */
    public void setEnableOBRBundleEvents(boolean enableOBRBundleEvents) {
        this.enableOBRBundleEvents = enableOBRBundleEvents;
    }

    /**
     * @return the enableObrEvents
     */
    @Override
    public boolean isEnableObrEvents() {
        return enableObrEvents;
    }

    /**
     * @param enableObrEvents the enableObrEvents to set
     */
    public void setEnableObrEvents(boolean enableObrEvents) {
        this.enableObrEvents = enableObrEvents;
    }

    /**
     * @return the synchronizers
     */
    public List<? extends Synchronizer> getSynchronizers() {
        return synchronizers;
    }

    /**
     * @param synchronizers the synchronizers to set
     */
    public void setSynchronizers(List<? extends Synchronizer> synchronizers) {
        this.synchronizers = synchronizers;
    }

    /**
     * @return the hazelcastConfig
     */
    public String getHazelcastConfig() {
        return hazelcastConfig;
    }

    /**
     * @param hazelcastConfig the hazelcastConfig to set
     */
    public void setHazelcastConfig(String hazelcastConfig) {
        this.hazelcastConfig = hazelcastConfig;
    }

    /**
     * @return the bundleClassLoader
     */
    public BundleClassLoader getBundleClassLoader() {
        return bundleClassLoader;
    }

    /**
     * @param bundleClassLoader the bundleClassLoader to set
     */
    public void setBundleClassLoader(BundleClassLoader bundleClassLoader) {
        this.bundleClassLoader = bundleClassLoader;
    }
}
