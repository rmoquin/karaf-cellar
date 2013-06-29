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
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import java.io.FileNotFoundException;
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
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.hazelcast.internal.BundleClassLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster manager implementation powered by Hazelcast.
 */
public class HazelcastClusterManager implements ClusterManager, EntryListener<String, Object> {
    private static transient Logger LOGGER = LoggerFactory.getLogger(HazelcastClusterManager.class);
    private String hazelcastConfig;
    private BundleClassLoader bundleClassLoader;
    private CellarCluster mainCluster;
    private Map<String, CellarCluster> clustersByName = new ConcurrentHashMap<String, CellarCluster>();
    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, EventProducer> clusterProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> clusterConsumers = new HashMap<String, EventConsumer>();
    private BundleContext bundleContext;
    private EventTransportFactory eventTransportFactory;

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
     * Get a Topic from the main administrative cluster.
     *
     * @param topicName the Topic name.
     * @return the Topic with the specifed name.
     */
    public ITopic getTopic(String topicName) {
        return ((HazelcastClusterManager) this.mainCluster).getTopic(topicName);
    }

    /**
     * Get a Queue from the main administrative cluster.
     *
     * @param queueName the Queue name.
     * @return the Queue with the specifed name.
     */
    public IQueue getQueue(String queueName) {
        return ((HazelcastClusterManager) this.mainCluster).getQueue(queueName);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        return this.mainCluster.listNodes();
    }

    /**
     * Get the nodes with given IDs.
     *
     * @param ids a collection of IDs to look for.
     * @return a Set containing the nodes.
     */
    @Override
    public Set<Node> listNodes(Collection<String> ids) {
        Set<Node> nodeList = new HashSet<Node>();
        if (ids != null && !ids.isEmpty()) {
            Set<Node> nodes = this.mainCluster.listNodes();
            for (Node node : nodes) {
                if (ids.contains(node.getId())) {
                    nodeList.add(node);
                }
            }
        }
        return nodeList;
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

    public void init() {
        this.createCluster(Configurations.DEFAULT_CLUSTER_NAME);
        clusterConfigurations.addEntryListener(this, true);
    }

    public void destroy() {

        // shutdown the group consumer/producers
        for (Map.Entry<String, EventConsumer> consumerEntry : clusterConsumers.entrySet()) {
            EventConsumer eventCons = consumerEntry.getValue();
            eventCons.stop();
        }
        clusterConsumers.clear();
        clusterProducers.clear();
        //TODO This should be leave cluster.
        for (Map.Entry<String, CellarCluster> entry : clustersByName.entrySet()) {
            entry.getValue().shutdown();
        }
    }

    @Override
    public String generateId() {
        CellarCluster cluster = this.getMasterCluster();
        return cluster.generateId();
    }

    @Override
    public void createCluster(String clusterName) {
        Group group = listGroups().get(groupName);
        if (group == null) {
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

            registerCluster(clusterName, cluster);
        }
    }

    @Override
    public void deleteCluster(String clusterName) {
        if (!clusterName.equals(Configurations.DEFAULT_GROUP_NAME)) {
            listGroups().remove(clusterName);
            persist(clusterName, false);
        } else {
            LOGGER.info("This node cannot leave the default cluster.");
        }
    }

    @Override
    public Set<CellarCluster> listAllClusters() {
        new HashSet<Group>(listGroups().values());
    }

    @Override
    public CellarCluster findClusterByName(String clusterName) {
        return listGroups().get(clusterName);
    }

    @Override
    public Set<CellarCluster> listClusters(Node node) {
        Set<CellarCluster> result = new HashSet<CellarCluster>();

        Map<String, CellarCluster> groupMap = this.getMap(Configurations.GROUPS);
        Collection<CellarCluster> groupCollection = groupMap.values();
        if (groupCollection != null && !groupCollection.isEmpty()) {
            for (CellarCluster group : groupCollection) {
                if (group.listNodes().contains(node)) {
                    return group;
                }
            }
        }
        return null;
    }

    protected void registerCluster(String clusterName, HazelcastCluster cluster) {
        try {
            Properties serviceProperties = new Properties();
            serviceProperties.put("type", "cluster");
            serviceProperties.put("name", clusterName);
            if (!producerRegistrations.containsKey(clusterName)) {
                EventProducer eventProducer = clusterProducers.get(clusterName);
                if (eventProducer == null) {
                    eventProducer = eventTransportFactory.getEventProducer(clusterName, Boolean.TRUE);
                    clusterProducers.put(clusterName, eventProducer);
                }
                ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), eventProducer, (Dictionary) serviceProperties);
                producerRegistrations.put(clusterName, producerRegistration);
                cluster.setEventProducer(eventProducer);
            }

            if (!consumerRegistrations.containsKey(clusterName)) {
                EventConsumer eventConsumer = clusterConsumers.get(clusterName);
                if (eventConsumer == null) {
                    eventConsumer = eventTransportFactory.getEventConsumer(clusterName, true);
                    clusterConsumers.put(clusterName, eventConsumer);
                } else if (!eventConsumer.isConsuming()) {
                    eventConsumer.start();
                }
                ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), eventConsumer, (Dictionary) serviceProperties);
                consumerRegistrations.put(clusterName, consumerRegistration);
            }
            persist(clusterName, true);
            listGroups().put(clusterName, cluster);
        } catch (Exception ex) {
            LOGGER.error("Error initializing the cluster manager: ", ex);
            throw new RuntimeException(ex);
        }
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
        persist(clusterName, false);
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
        clusterProducers.remove(clusterName);
        EventConsumer eventConsumer = clusterConsumers.remove(clusterName);
        if (eventConsumer != null) {
            eventConsumer.stop();
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
    
    @Override
    public CellarCluster getMasterCluster() {
        return this.mainCluster;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
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
