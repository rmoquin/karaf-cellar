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

import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.CommandExecutionContext;
import org.apache.karaf.cellar.core.tasks.GroupTaskResult;
import org.apache.karaf.cellar.core.tasks.ManageGroupTask;
import org.apache.karaf.cellar.hazelcast.HazelcastCluster;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;

/**
 *
 * @author rmoquin
 */
public class CommandExecutionContextImpl implements CommandExecutionContext {
    private String name;
    private HazelcastCluster cluster;
    private IExecutorService executorService;

    public void init() {
        executorService = this.cluster.getExecutorService(name);
    }

    public void destroy() {
    }

    @Override
    public Map<Node, Future<GroupTaskResult>> execute(ManageGroupTask command, Set<Node> destinations) {
        Map<Node, Future<GroupTaskResult>> results = new HashMap<Node, Future<GroupTaskResult>>();
        if (destinations.size() == 1) {
            Node node = destinations.iterator().next();
            Member member = cluster.findMemberById(node.getId());
            Future<GroupTaskResult> result = this.executorService.submitToMember(command, member);
            results.put(node, result);
        } else {
            Set<Member> members = new HashSet<Member>();
            for (Iterator<Node> it = destinations.iterator(); it.hasNext();) {
                Node node = it.next();
                Member member = cluster.findMemberById(node.getId());
                members.add(member);
            }
            Map<Member, Future<GroupTaskResult>> executedResult = this.executorService.submitToMembers(command, members);
            for (Map.Entry<Member, Future<GroupTaskResult>> entry : executedResult.entrySet()) {
                Member member = entry.getKey();
                Future<GroupTaskResult> future = entry.getValue();
                results.put(new HazelcastNode(member), future);
            }
        }
        return results;
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
}
