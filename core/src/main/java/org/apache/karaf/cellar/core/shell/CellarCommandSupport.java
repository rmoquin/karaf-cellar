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
package org.apache.karaf.cellar.core.shell;

import java.util.Map;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.command.Result;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * Abstract Cellar command.
 */
public abstract class CellarCommandSupport extends OsgiCommandSupport {

    protected ClusterManager clusterManager;
    protected GroupManager groupManager;
    protected DistributedExecutionContext executionContext;

    public void printTaskResults(Map<Node, Result> results) {
        for (Map.Entry<Node, Result> response : results.entrySet()) {
            Node node = response.getKey();
            Result featureEventResult = response.getValue();
            System.err.println("Node, " + node.getName() + " task successful: " + featureEventResult.isSuccessful());
            if (featureEventResult.getThrowable() != null) {
                System.err.println("Task error details: ");
                featureEventResult.getThrowable().printStackTrace(System.err);
            }
        }
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /**
     * @return the executionContext
     */
    public DistributedExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * @param executionContext the executionContext to set
     */
    public void setExecutionContext(DistributedExecutionContext executionContext) {
        this.executionContext = executionContext;
    }
}
