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
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class HazelcastCluster implements CellarCluster, MembershipListener {
    @JsonIgnore
    private static Logger LOGGER = LoggerFactory.getLogger(HazelcastCluster.class);
    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";
    @JsonIgnore
    private IdGenerator idgenerator;
    @JsonIgnore
    private HazelcastInstance instance;
    private String name;
    @JsonIgnore
    private boolean sychronizer;
    @JsonIgnore
    private String listenerId;
    private HazelcastNode localNode;
    @JsonIgnore
    private Map<String, HazelcastNode> memberNodes = new ConcurrentHashMap<String, HazelcastNode>();
    @JsonIgnore
    private List<? extends Synchronizer> synchronizers;
    @JsonIgnore
    private EventProducer eventProducer;

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
        return true;
    }

    public void init(String name, Config config, boolean synchronizer) {
        this.name = name;
        this.sychronizer = synchronizer;
        instance = Hazelcast.newHazelcastInstance(config);
        listenerId = instance.getCluster().addMembershipListener(this);
        this.localNode = new HazelcastNode(instance.getCluster().getLocalMember());
        memberNodes.put(this.localNode.getId(), this.localNode);
    }
    
    /**
     * Sends a cluster event from this cluster as the source of the event.
     * 
     * @param event to send
     */
    @Override
    public void produce(Event event) {
        this.eventProducer.produce(event);
    }

    @Override
    public boolean emitsEvents() {
        return eventProducer.getSwitch().getStatus().equals(SwitchStatus.ON);
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
        String uuid = membershipEvent.getMember().getUuid();
        HazelcastNode node = this.memberNodes.remove(uuid);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Node , " + uuid + ", left cluster, " + this.name + ".");
        }
        node.destroy();
    }

    @Override
    public void shutdown() {
        this.eventProducer = null;
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
    public Set getSet(String setName) {
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
     * @return the sychronizer
     */
    @Override
    public boolean isSychronizer() {
        return sychronizer;
    }

    /**
     * @param sychronizer the sychronizer to set
     */
    public void setSychronizer(boolean sychronizer) {
        this.sychronizer = sychronizer;
    }

    /**
     * @return the eventProducer
     */
    public EventProducer getEventProducer() {
        return eventProducer;
    }

    /**
     * @param eventProducer the eventProducer to set
     */
    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }
}
