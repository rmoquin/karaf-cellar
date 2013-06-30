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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.hazelcast.factory.HazelcastConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class HazelcastCluster implements CellarCluster, MembershipListener {
    @JsonIgnore
    private static Logger LOGGER = LoggerFactory.getLogger(HazelcastCluster.class);
    private HazelcastConfigurationManager configManager;
    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";
    @JsonIgnore
    private IdGenerator idgenerator;
    @JsonIgnore
    private HazelcastInstance instance;
    private String name;
    @JsonIgnore
    private String listenerId;
    private HazelcastNode localNode;
    private Config config;
    @JsonIgnore
    private Map<String, HazelcastNode> memberNodes = new ConcurrentHashMap<String, HazelcastNode>();

    public void init() {
        this.config = this.configManager.getHazelcastConfig(this.name);
        this.instance = Hazelcast.newHazelcastInstance(config);
        this.listenerId = instance.getCluster().addMembershipListener(this);
        this.localNode = new HazelcastNode(instance.getCluster().getLocalMember());
        this.memberNodes.put(this.localNode.getId(), this.localNode);
    }
    
    @Override
    public void shutdown() {
        instance.getCluster().removeMembershipListener(listenerId);
        if (instance != null) {
            instance.getLifecycleService().shutdown();
        }
    }

    public String addMembershipListener(MembershipListener listener) {
        return instance.getCluster().addMembershipListener(listener);
    }

    public boolean removeMembershipListener(String registrationId) {
        return instance.getCluster().removeMembershipListener(registrationId);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        Set<Node> nodes = new HashSet<Node>();
        nodes.addAll(this.memberNodes.values());
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
            for (String id : ids) {
                Node node = this.memberNodes.get(id);
                if (node != null) {
                    nodes.add(node);
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
            return this.memberNodes.get(id);
        }
        return null;
    }

    /**
     * Returns whether or not this cluster contains a node with the specified id.
     *
     * @param id the node ID.
     * @return true if there is a node with that id.
     */
    @Override
    public boolean hasNodeWithId(String id) {
        if (id != null) {
            return this.memberNodes.containsKey(id);
        }
        return false;
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

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        HazelcastNode newNode = new HazelcastNode(member);
        this.memberNodes.put(newNode.getId(), newNode);
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        String uuid = membershipEvent.getMember().getUuid();
        HazelcastNode node = this.memberNodes.remove(uuid);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Node , " + uuid + ", left cluster, " + this.name + ".");
        }
        node.destroy();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HazelcastCluster other = (HazelcastCluster) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Get a Map in Hazelcast.
     *
     * @param mapName the Map name.
     * @return the Map in Hazelcast.
     */
    @Override
    public IMap getMap(String mapName) {
        return instance.getMap(mapName);
    }

    /**
     * Get a List in Hazelcast.
     *
     * @param listName the List name.
     * @return the List in Hazelcast.
     */
    @Override
    public IList getList(String listName) {
        return instance.getList(listName);
    }

    /**
     * Get a Topic in Hazelcast.
     *
     * @param topicName the Topic name.
     * @return the Topic in Hazelcast.
     */
    public ITopic getTopic(String topicName) {
        return instance.getTopic(topicName);
    }

    /**
     * Get a Queue in Hazelcast.
     *
     * @param queueName the Queue name.
     * @return the Queue in Hazelcast.
     */
    public IQueue getQueue(String queueName) {
        return instance.getQueue(queueName);
    }

    /**
     * Get a Set in Hazelcast.
     *
     * @param setName the Set name.
     * @return the Set in Hazelcast.
     */
    @Override
    public ISet getSet(String setName) {
        return instance.getSet(setName);
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
     * @return the configManager
     */
    public HazelcastConfigurationManager getConfigManager() {
        return configManager;
    }

    /**
     * @param configManager the configManager to set
     */
    public void setConfigManager(HazelcastConfigurationManager configManager) {
        this.configManager = configManager;
    }
}
