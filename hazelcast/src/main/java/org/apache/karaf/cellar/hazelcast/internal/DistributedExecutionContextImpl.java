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
package org.apache.karaf.cellar.hazelcast.internal;

import org.apache.karaf.cellar.core.command.DistributedCallback;
import org.apache.karaf.cellar.core.command.DistributedMultiCallback;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MultiExecutionCallback;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.Command;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.command.DistributedResult;
import org.apache.karaf.cellar.core.command.DistributedTask;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class DistributedExecutionContextImpl<C extends Command, R extends DistributedResult> implements DistributedExecutionContext<C, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedExecutionContextImpl.class);
    private String name;
    private HazelcastInstance instance;
    private NodeConfiguration nodeConfiguration;
    private IExecutorService executorService;
    private int timeoutSeconds = 60;
    public String SWITCH_ID = "org.apache.karaf.cellar.executor.";

    public DistributedExecutionContextImpl() {
        this("default");
    }

    public DistributedExecutionContextImpl(String name) {
        this.name = name;
    }

    private Switch eventSwitch;

    public void init() {
        SWITCH_ID = SWITCH_ID + name;
        eventSwitch = new BasicSwitch(SWITCH_ID);
        executorService = this.instance.getExecutorService(name);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Executes the distributed task on the specified nodes and waits for them to finish.
     *
     * @param command the task to execute
     * @param destinations the destinations to send to.
     * @return
     */
    @Override
    public Map<Node, R> execute(C command, Set<Node> destinations) {
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        if (destinations == null || destinations.isEmpty()) {
            LOGGER.debug("No destination nodes were provided to execute command, {}", command);
            return finishedResults;
        }
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = findMemberById(node.getId());
            members.add(member);
        }
        DistributedTask task = new DistributedTask(command);
        Map<Member, Future<R>> executedResult = this.executorService.submitToMembers(task, members);
        for (Map.Entry<Member, Future<R>> entry : executedResult.entrySet()) {
            Member member = entry.getKey();
            Node node = new HazelcastNode(member);
            try {
                R result = entry.getValue().get(timeoutSeconds, TimeUnit.SECONDS);
                finishedResults.put(node, result);
//                LOGGER.info("Task completed on node {} with result {} for command {}.", node, result, command);
            } catch (Exception ex) {
                LOGGER.error("Node {} generated an error executing task {}", node, command, ex);
            }
        }
        printTaskResults(finishedResults);
        LOGGER.info("All tasks completed for command {}.", command);
        return finishedResults;
    }

    /**
     * Executes the distributed task on the specified nodes and waits for them to finish.
     *
     * @param command the task to execute
     * @param destination the destinations to sent to.
     * @return
     */
    @Override
    public Map<Node, R> execute(C command, Node destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination node cannot be null, cannot execute command, " + command);
        }
        Member member = findMemberById(destination.getId());
        DistributedTask task = new DistributedTask(command);
        Future<R> future = this.executorService.submitToMember(task, member);
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        Node node = new HazelcastNode(member);
        try {
            R result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            finishedResults.put(node, result);
//            LOGGER.info("Task completed on node {} with result {} for command {}.", destination, result, command);
        } catch (Exception ex) {
            LOGGER.error("Node {} generated an error executing task {}", node, command, ex);
        }
        printTaskResults(finishedResults);
        LOGGER.info("All tasks completed for command {}.", command);
        return finishedResults;
    }

    /**
     * Executes the distributed task on the specified nodes and waits for them to finish.
     *
     * @param command the task to execute
     * @param destinations the destinations to sent to.
     * @return
     */
    @Override
    public Map<Node, R> executeAndWait(C command, Set<Node> destinations) {
        Map<Node, R> results = this.execute(command, destinations);
        return results;
    }

    /**
     * Executes the distributed task on the specified node and waits for it to finish.
     *
     * @param command the task to execute
     * @param destination the destination to sent to.
     * @return a map containing the results from each node.
     */
    @Override
    public Map<Node, R> executeAndWait(C command, Node destination) {
        Map<Node, R> results = this.execute(command, destination);
        return results;
    }

    /**
     * Executes the distributed task on the specified node asynchonously with the specified callback handling each
     * results.
     *
     * @param command the task to execute
     * @param destination the destin
     * @param callback
     */
    @Override
    public void executeAsync(C command, Node destination, DistributedCallback<R> callback) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination node cannot be null, cannot execute command, " + command);
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        ExecutionCallback<R> distributedCallback = new DistributedCallbackImpl<R>(callback);
        Member member = findMemberById(destination.getId());
        DistributedTask task = new DistributedTask(command);
        this.executorService.submitToMember(task, member, distributedCallback);
    }

    @Override
    public void executeAsync(C command, Set<Node> destinations, DistributedMultiCallback callback) {
        if (destinations == null || destinations.isEmpty()) {
            LOGGER.debug("No destination nodes were provided to execute command, {}", command);
            return;
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        MultiExecutionCallback distributedCallback = new DistributedMultiCallbackImpl(callback);
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = findMemberById(node.getId());
            members.add(member);
        }
        DistributedTask task = new DistributedTask(command);
        this.executorService.submitToMembers(task, members, distributedCallback);
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        boolean status = nodeConfiguration.isProducer();
        if (status) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
    }

    private void printTaskResults(Map<Node, R> results) {
        LOGGER.info("Printing results from distributed tasks: " + results);
        for (Map.Entry<Node, R> response : results.entrySet()) {
            Node node = response.getKey();
            R featureEventResult = response.getValue();
            LOGGER.info("Node, " + node.getName() + " task successful: " + featureEventResult.isSuccessful());
            if (featureEventResult.getThrowable() != null) {
                LOGGER.error("Task error details: ", featureEventResult.getThrowable());
            }
        }
    }

    private Member findMemberById(String id) {
        Set<Member> members = instance.getCluster().getMembers();
        for (Member member : members) {
            if (member.getUuid().equals(id)) {
                return member;
            }
        }
        return null;
    }

    @Override
    public String getTopic() {
        return name;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the timeoutSeconds
     */
    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @param timeoutSeconds the timeoutSeconds to set
     */
    @Override
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
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
}
