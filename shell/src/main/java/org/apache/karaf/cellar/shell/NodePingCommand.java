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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.utils.ping.Ping;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import org.apache.karaf.cellar.core.command.DistributedExecutionContext;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

@Command(scope = "cluster", name = "node-ping", description = "Ping a cluster node")
public class NodePingCommand extends CellarCommandSupport {

    private static Long TIMEOUT = 5000L;

    @Argument(index = 0, name = "node", description = "The ID of the node to ping", required = true, multiValued = false)
    String nodeName;

    @Argument(index = 1, name = "iterations", description = "The number of iterations to perform", required = false, multiValued = false)
    Integer iterations = 10;

    @Argument(index = 2, name = "interval", description = "The time in millis to wait between iteration", required = false, multiValued = false)
    Long interval = 1000L;

    private DistributedExecutionContext executionContext;

    @Override
    protected Object doExecute() throws Exception {
        Node node = clusterManager.findNodeByName(nodeName);
        if (node == null) {
            System.out.println("Cluster node with name " + nodeName + " doesn't exist");
            return null;
        }

        System.out.println("PING " + nodeName);
        try {
            for (int i = 1; i <= iterations; i++) {
                Long start = System.currentTimeMillis();
                Ping ping = new Ping();
                //Don't care about the Pong result object.
                executionContext.executeAndHandle(ping, node);
                Long stop = System.currentTimeMillis();
                Long delay = stop - start;
                if (delay >= TIMEOUT) {
                    System.err.println(String.format("TIMEOUT %s %s", i, nodeName));
                } else {
                    System.out.println(String.format("from %s: req=%s time=%s ms", i, nodeName, delay));
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
            // nothing to do
        } catch (Exception e) {
            throw e;
        }
        return null;
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
