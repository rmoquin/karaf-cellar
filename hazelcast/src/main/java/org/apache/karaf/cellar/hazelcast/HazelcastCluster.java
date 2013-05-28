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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.slf4j.Logger;

/**
 *
 * @author rmoquin
 */
public class HazelcastCluster implements CellarCluster, Serializable, MembershipListener {
    private static transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastCluster.class);
    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";
    private IdGenerator idgenerator;
    private transient HazelcastInstance instance;
    private String hazelcastConfig;
    private String name;
    private String listenerId;
    private HazelcastNode localNode;
    private ConcurrentHashMap<String, HazelcastNode> memberNodes;
    private List<? extends Synchronizer> synchronizers;
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
        Config cfg = null;
        try {
            cfg = new FileSystemXmlConfig(hazelcastConfig);
        } catch (FileNotFoundException ex) {
            LOGGER.error("The default hazelcast config couldn't be found, hazelcast will try to drop back to it's internal default.", ex);
        }
        if (cfg == null) {
            cfg = new Config();
        }
        cfg.getGroupConfig().setName(name);
        instance = Hazelcast.newHazelcastInstance(cfg);
        instance.getCluster().addMembershipListener(this);
        this.localNode = new HazelcastNode(instance.getCluster().getLocalMember());
        memberNodes.put(this.localNode.getId(), this.localNode);
        /*Bundle b = FrameworkUtil.getBundle(com.hazelcast.config.Config.class);
         ClassLoader priorClassLoader = Thread.currentThread().getContextClassLoader();

         try {
         Thread.currentThread().setContextClassLoader(new BundleClassLoader(b));
         instance = Hazelcast.newHazelcastInstance(config);
         } finally {
         Thread.currentThread().setContextClassLoader(priorClassLoader);
         }
         }*/
    }
    
    @Override
    public void start() {
        //This didn't do anything previously and probably doesn't now.
    }

    @Override
    public void stop() {
        //TODO I forget what this was supposed to do.
    }

    public void update(Map<String, Object> props) {
        LOGGER.warn("Hazelcast cluster: " + name + " update method was called with properties: " + props);
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        try {
            HazelcastNode newNode = new HazelcastNode(member);
            this.memberNodes.put(newNode.getId(), newNode);
            if (synchronizers != null && !synchronizers.isEmpty()) {
                for (Synchronizer synchronizer : synchronizers) {
                    if (synchronizer.isSyncEnabled(this)) {
                        synchronizer.synchronize(this);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error while adding memberAdded", e);
        }
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        // nothing to do
    }

    public void shutdown() {
        instance.getCluster().removeMembershipListener(listenerId);
        if (instance != null) {
            instance.getLifecycleService().shutdown();
        }
    }

    /**
     * Get a Map in Hazelcast.
     *
     * @param mapName the Map name.
     * @return the Map in Hazelcast.
     */
    @Override
    public Map getMap(String mapName) {
        return instance.getMap(mapName);
    }

    /**
     * Get a List in Hazelcast.
     *
     * @param listName the List name.
     * @return the List in Hazelcast.
     */
    @Override
    public List getList(String listName) {
        return instance.getList(listName);
    }

    /**
     * Get a Set in Hazelcast.
     *
     * @param setName the Set name.
     * @return the Set in Hazelcast.
     */
    @Override
    public Set getSet(String setName) {
        return instance.getSet(setName);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        Set<Node> nodes = new HashSet<Node>();

        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Set<Member> members = cluster.getMembers();
            if (members != null && !members.isEmpty()) {
                for (Member member : members) {
                    HazelcastNode node = new HazelcastNode(member);
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /**
     * Get the nodes with given IDs.
     *
     * @param ids a collection of IDs to look for.
     * @return a Set containing the nodes.
     */
    @Override
    public Set<Node> listNodes(Collection<String> ids) {
        Set<Node> nodes = new HashSet<Node>();
        if (ids != null && !ids.isEmpty()) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null) {
                    for (Member member : members) {
                        if (ids.contains(member.getUuid())) {
                            HazelcastNode node = new HazelcastNode(member);
                            nodes.add(node);
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * Get a node with a given ID.
     *
     * @param id the node ID.
     * @return the node.
     */
    @Override
    public Node findNodeById(String id) {
        if (id != null) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        if (id.equals(member.getUuid())) {
                            return new HazelcastNode(member);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Generate an unique ID.
     *
     * @return the generated unique ID.
     */
    @Override
    public synchronized String generateId() {
        if (idgenerator == null) {
            idgenerator = instance.getIdGenerator(GENERATOR_ID);
        }
        return String.valueOf(idgenerator.newId());
    }

    /**
     * @return the instance
     */
    public HazelcastInstance getInstance() {
        return instance;
    }

    /**
     * @param instance the instance to set
     */
    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    /**
     * @return the clusterName
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param clusterName the clusterName to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the localNode
     */
    @Override
    public HazelcastNode getLocalNode() {
        return localNode;
    }

    /**
     * @param localNode the localNode to set
     */
    public void setLocalNode(HazelcastNode localNode) {
        this.localNode = localNode;
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
}
