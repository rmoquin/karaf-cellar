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
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.Command;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.command.DistributedResult;
import org.apache.karaf.cellar.core.command.DistributedTask;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.hazelcast.HazelcastCluster;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class DistributedExecutionContextImpl<C extends Command, R extends DistributedResult> implements DistributedExecutionContext<C, R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedExecutionContextImpl.class);
    private String name;
    private HazelcastCluster cluster;
    private IExecutorService executorService;
    private ConfigurationAdmin configurationAdmin;
    private int timeoutSeconds = 60;
    public static final String SWITCH_ID = "org.apache.karaf.cellar.topic.producer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    public void init() {
        executorService = this.cluster.getExecutorService(name);
    }

    public void destroy() {
        //I think the executor service shuts itself down automatically.
    }

    /**
     * Executes the distributed task and waits for each Future to return rather than let the caller manually handle it.
     *
     * @param command the task to execute
     * @param destinations the destinations to sent to.
     * @return
     */
    @Override
    public Map<Node, R> executeAndWait(C command, Set<Node> destinations) {
        Map<Node, Future<R>> results = this.execute(command, destinations);
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        for (Map.Entry<Node, Future<R>> entry : results.entrySet()) {
            Node node = entry.getKey();
            try {
                R result = entry.getValue().get(timeoutSeconds, TimeUnit.SECONDS);
                finishedResults.put(node, result);
            } catch (Exception ex) {
                LOGGER.error("Node {} generated an error executing task {}", node.getName(), command, ex);
            }
        }
        LOGGER.info("Completed task {} on selected nodes." + command);
        return finishedResults;
    }

    /**
     * Executes the distributed task and waits for each Future to return rather than let the caller manually handle it.
     *
     * @param command the task to execute
     * @param destination the destination to sent to.
     * @return
     */
    @Override
    public Map<Node, R> executeAndWait(C command, Node destination) {
        Map<Node, Future<R>> results = this.execute(command, destination);
        Map<Node, R> finishedResults = new HashMap<Node, R>();
        Map.Entry<Node, Future<R>> entry = results.entrySet().iterator().next();
        Node node = entry.getKey();
        try {
            R result = entry.getValue().get(timeoutSeconds, TimeUnit.SECONDS);
            finishedResults.put(node, result);
        } catch (Exception ex) {
            LOGGER.error("Node {} generated an error executing task {}", node.getName(), command, ex);
        }
        LOGGER.info("Completed task {}" + command);
        return finishedResults;
    }

    @Override
    public Map<Node, Future<R>> execute(C command, Node node) {
        Map<Node, Future<R>> results = new HashMap<Node, Future<R>>();
        Member member = cluster.findMemberById(node.getId());
        Future<R> result;
        DistributedTask task = new DistributedTask(command);
        result = this.executorService.submitToMember(task, member);
        results.put(node, result);
        return results;
    }

    @Override
    public void executeAsync(C command, Node node, DistributedCallback<R> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        ExecutionCallback<R> distributedCallback = new DistributedCallbackImpl<R>(callback);
        Member member = cluster.findMemberById(node.getId());
        DistributedTask task = new DistributedTask(command);
        this.executorService.submitToMember(task, member, distributedCallback);
    }

    @Override
    public Map<Node, Future<R>> execute(C command, Set<Node> destinations) {
        Map<Node, Future<R>> results = new HashMap<Node, Future<R>>();
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = cluster.findMemberById(node.getId());
            members.add(member);
        }
        DistributedTask task = new DistributedTask(command);
        Map<Member, Future<R>> executedResult = this.executorService.submitToMembers(task, members);
        for (Map.Entry<Member, Future<R>> entry : executedResult.entrySet()) {
            Member member = entry.getKey();
            Future<R> future = entry.getValue();
            results.put(new HazelcastNode(member), future);
        }
        LOGGER.info("Completed task {}" + command);
        return results;
    }

    @Override
    public void executeAsync(C command, Set<Node> destinations, DistributedMultiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null.");
        }
        MultiExecutionCallback distributedCallback = new DistributedMultiCallbackImpl(callback);
        Set<Member> members = new HashSet<Member>();
        for (Node node : destinations) {
            Member member = cluster.findMemberById(node.getId());
            members.add(member);
        }
        DistributedTask task = new DistributedTask(command);
        this.executorService.submitToMembers(task, members, distributedCallback);
    }

    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP_MEMBERSHIP_LIST_DO_STORE);
            if (configuration != null) {
                Boolean status = Boolean.valueOf((String) configuration.getProperties().get(Configurations.PRODUCER));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving switch, {}, for execution context.", Configurations.PRODUCER, e);
        }
        return eventSwitch;
    }

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

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
