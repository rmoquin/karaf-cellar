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

import com.hazelcast.core.ClientService;
import com.hazelcast.core.DistributedObjectListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISemaphore;
import com.hazelcast.core.ISet;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastCluster.class);
    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";
    private IdGenerator idgenerator;
    private HazelcastInstance instance;
    private HazelcastConfigurationManager configManager;
    private String name;
    private String nodeName;
    private String memberListenerId;
    private HazelcastNode localNode;
    private final Map<String, HazelcastNode> memberNodesByName = new ConcurrentHashMap<String, HazelcastNode>();
    private final Map<String, HazelcastNode> memberNodesById = new ConcurrentHashMap<String, HazelcastNode>();

    public void init() {
        try {
            this.instance = Hazelcast.newHazelcastInstance(configManager.createHazelcastConfig(name, nodeName));
            this.localNode = new HazelcastNode(instance.getCluster().getLocalMember());
            this.memberNodesByName.put(this.localNode.getName(), this.localNode);
            this.memberNodesById.put(this.localNode.getId(), this.localNode);
            this.memberListenerId = instance.getCluster().addMembershipListener(this);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("An error occurred creating instance: " + this.nodeName, ex);
        }
    }

    @Override
    public void shutdown() {
        instance.getCluster().removeMembershipListener(memberListenerId);
        this.memberNodesByName.clear();
        this.memberNodesById.clear();
        this.idgenerator = null;
        this.localNode.destroy();
        this.localNode = null;
        instance.shutdown();
        instance = null;
    }

    public String addMembershipListener(MembershipListener listener) {
        return instance.getCluster().addMembershipListener(listener);
    }

    public boolean removeMembershipListener(String registrationId) {
        return instance.getCluster().removeMembershipListener(registrationId);
    }

    public String addDistributedObjectListener(DistributedObjectListener listenerId) {
        return instance.addDistributedObjectListener(listenerId);
    }

    public boolean removeDistributedObjectListener(String listenerId) {
        return instance.removeDistributedObjectListener(listenerId);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        Set<Node> nodes = new HashSet<Node>();
        nodes.addAll(this.memberNodesByName.values());
        return nodes;
    }

    /**
     * Get the nodes with given namess.
     *
     * @param names
     */
    @Override
    public Set<Node> listNodes(Collection<String> names) {
        Set<Node> nodes = new HashSet<Node>();
        if (names != null && !names.isEmpty()) {
            for (String n : names) {
                Node node = this.memberNodesByName.get(n);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    public Member findMemberById(String id) {
        Set<Member> members = instance.getCluster().getMembers();
        for (Member member : members) {
            if (member.getUuid().equals(id)) {
                return member;
            }
        }
        return null;
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
            return this.memberNodesById.get(id);
        }
        return null;
    }

    /**
     * Geta a node by it's instance name..
     *
     * @param name the instance name..
     * @return the node.
     */
    @Override
    public Node findNodeByName(String name) {
        if (name != null) {
            return this.memberNodesByName.get(name);
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
            return this.memberNodesById.containsKey(id);
        }
        return false;
    }

    /**
     * Returns whether or not this cluster contains a node with the specified name.
     *
     * @param name the node name.
     * @return true if there is a node with that name.
     */
    @Override
    public boolean hasNodeWithName(String name) {
        if (name != null) {
            return this.memberNodesByName.containsKey(name);
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

    public ILock getLock(String s) {
        return instance.getLock(s);
    }

    public IExecutorService getExecutorService(String name) {
        return instance.getExecutorService(name);
    }

    public ICountDownLatch getCountDownLatch(String name) {
        return instance.getCountDownLatch(name);
    }

    public ISemaphore getSemaphore(String name) {
        return instance.getSemaphore(name);
    }

    public ClientService getClientService() {
        return instance.getClientService();
    }

    public ConcurrentMap<String, Object> getUserContext() {
        return instance.getUserContext();
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        addNewNode(member);
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        String uuid = membershipEvent.getMember().getUuid();
        HazelcastNode node = this.memberNodesById.remove(uuid);
        if (node != null) {
            this.memberNodesByName.remove(node.getName());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Node , " + node.getName() + ", left cluster, " + this.name + ".");
            }
            node.destroy();
        } else {
            LOGGER.info("Node with uuid {} couldn't be found to be removed.", uuid);
        }
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
        return !((this.name == null) ? (other.name != null) : !this.name.equals(other.name));
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
     * @param name the clusterName to set
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

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName the nodeName to set
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    private void addNewNode(Member member) {
        HazelcastNode newNode = new HazelcastNode(member);
        this.memberNodesByName.put(newNode.getName(), newNode);
        this.memberNodesById.put(newNode.getId(), newNode);
    }
}
