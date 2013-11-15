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
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.command.DistributedResult;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.hazelcast.HazelcastCluster;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class DistributedExecutionContextImpl<T extends Event, R extends DistributedResult> implements DistributedExecutionContext<T, R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedExecutionContextImpl.class);
    private String name;
    private HazelcastCluster cluster;
    private IExecutorService executorService;
    private int timeoutSeconds = 60;

    public void init() {
        executorService = this.cluster.getExecutorService(name);
    }

    public void destroy() {
    }

    /**
     * Executes the distributed task and waits for each Future to return rather than let the caller manually handle it.
     *
     * @param task the task to execute
     * @param destinations the destinations to sent to.
     * @return
     */
    @Override
    public Map<Node, R> executeAndWait(T task, Set<Node> destinations) {
        Map<Node, Future<R>> results = this.execute(task, destinations);
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        for (Map.Entry<Node, Future<R>> entry : results.entrySet()) {
            Node node = entry.getKey();
            try {
                R result = entry.getValue().get(timeoutSeconds, TimeUnit.SECONDS);
                finishedResults.put(node, result);
            } catch (Exception ex) {
                LOGGER.error("Node {} generated an error executing task {}", node.getName(), task, ex);
            }
        }
        LOGGER.info("Completed task {} on selected nodes." + task);
        return finishedResults;
    }

    /**
     * Executes the distributed task and waits for each Future to return rather than let the caller manually handle it.
     *
     * @param task the task to execute
     * @param destination the destination to sent to.
     * @return
     */
    @Override
    public Map<Node, R> executeAndWait(T task, Node destination) {
        Map<Node, Future<R>> results = this.execute(task, destination);
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        Map.Entry<Node, Future<R>> entry = results.entrySet().iterator().next();
        Node node = entry.getKey();
        try {
            R result = entry.getValue().get(timeoutSeconds, TimeUnit.SECONDS);
            finishedResults.put(node, result);
        } catch (Exception ex) {
            LOGGER.error("Node {} generated an error executing task {}", node.getName(), task, ex);
        }
        LOGGER.info("Completed task {}" + task);
        return finishedResults;
    }

    @Override
    public Map<Node, Future<R>> execute(T task, Node node) {
        Map<Node, Future<R>> results = new HashMap<Node, Future<R>>();
        Member member = cluster.findMemberById(node.getId());
        Future<R> result;
        result = this.executorService.submitToMember(task, member);
        results.put(node, result);
        return results;
    }

    @Override
    public void executeAsync(T task, Node node, DistributedCallback<R> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        ExecutionCallback<R> distributedCallback = new DistributedCallbackImpl<R>(callback);
        Member member = cluster.findMemberById(node.getId());
        this.executorService.submitToMember(task, member, distributedCallback);
    }

    @Override
    public Map<Node, Future<R>> execute(T task, Set<Node> destinations) {
        Map<Node, Future<R>> results = new HashMap<Node, Future<R>>();
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = cluster.findMemberById(node.getId());
            members.add(member);
        }
        Map<Member, Future<R>> executedResult = this.executorService.submitToMembers(task, members);
        for (Map.Entry<Member, Future<R>> entry : executedResult.entrySet()) {
            Member member = entry.getKey();
            Future<R> future = entry.getValue();
            results.put(new HazelcastNode(member), future);
        }
        LOGGER.info("Completed task {}" + task);
        return results;
    }

    @Override
    public void executeAsync(T task, Set<Node> destinations, DistributedMultiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        MultiExecutionCallback distributedCallback = new DistributedMultiCallbackImpl(callback);
        Map<Node, Future<R>> results = new HashMap<Node, Future<R>>();
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = cluster.findMemberById(node.getId());
            members.add(member);
        }
        this.executorService.submitToMembers(task, members, distributedCallback);
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
     * @return the cluster
     */
    public HazelcastCluster getCluster() {
        return cluster;
    }

    /**
     * @param cluster the cluster to set
     */
    public void setCluster(HazelcastCluster cluster) {
        this.cluster = cluster;
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
}
