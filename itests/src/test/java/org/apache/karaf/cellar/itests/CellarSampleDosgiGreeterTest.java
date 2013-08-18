/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.itests;

import java.util.Set;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarSampleDosgiGreeterTest extends CellarTestSupport {

    @Test
    //@Ignore
    public void testDosgiGreeter() throws Exception {
        installCellar();
        createCellarChild("node1");
        if (!waitForInstanceToCluster(2)) {
            throw new Exception("Failed waiting for second node to connect to cluster..");
        }
        createCellarChild("node2");
        if (!waitForInstanceToCluster(3)) {
            throw new Exception("Failed waiting for third node to connect to cluster..");
        }

        System.out.println(executeCommand("feature:repo-add mvn:org.apache.karaf.cellar.samples/dosgi-greeter/3.0.0-SNAPSHOT/xml/features"));
        System.out.println(executeCommand("instance:list"));

        System.out.println(executeCommand("cluster:node-list"));
		ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
		Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 3 cellar nodes running", 3 <= nodes.size());
        String node1 = getNodeIdOfChild("node1");
        String node2 = getNodeIdOfChild("node2");

        System.out.println("Node 1: " + node1);
        System.out.println("Node 2: " + node2);

        executeCommand("cluster:group-create client-grp");
        executeCommand("cluster:group-create service-grp");
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:group-list"));
        System.out.println(executeCommand("cluster:group-set client-grp " + localNode.getId()));
        System.out.println(executeCommand("cluster:group-set service-grp " + node1));
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:feature-install client-grp greeter-client"));
        System.out.println(executeCommand("cluster:feature-install service-grp greeter-service"));
        Thread.sleep(5000);
        System.out.println(executeCommand("cluster:service-list"));

        String greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
        System.out.println(greetOutput);
        assertEquals("Expected 10 greets", 10, countGreetsFromNode(greetOutput, node1));
        System.out.println(executeCommand("cluster:group-set service-grp " + node2));
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:group-list"));
        System.out.println(executeCommand("cluster:list-services"));
        greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
        System.out.println(greetOutput);
        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node1));
        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node2));
    }

    public int countGreetsFromNode(String output, String nodeId) {
        int count = 0;
        String[] greets = output.split("\n");
        for (String greet : greets) {
            if (greet.contains(nodeId)) {
                count++;
            }
        }
        return count;
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("node1");
            destroyCellarChild("node2");
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
