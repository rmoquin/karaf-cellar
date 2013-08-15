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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarSampleCamelHazelcastTest extends CellarTestSupport {

    @Test
    //@Ignore
    public void testCamelSampleApp() throws Exception {
        installCellar();
        createCellarChild("node1");
        if (!waitForInstanceToCluster(2)) {
            throw new Exception("Failed waiting for second node to connect to cluster..");
        }
        createCellarChild("node2");
        if (!waitForInstanceToCluster(3)) {
            throw new Exception("Failed waiting for third node to connect to cluster..");
        }

        System.out.println(executeCommand("feature:repo-add mvn:org.apache.karaf.cellar.samples/camel-hazelcast-app/3.0.0-SNAPSHOT/xml/features"));
        System.out.println(executeCommand("instance:list"));
        System.out.println(executeCommand("cluster:node-list"));
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 3 Cellar nodes running", nodes.size() >= 3);

        String node1 = getNodeIdOfChild("node1");
        String node2 = getNodeIdOfChild("node2");

        executeCommand("cluster:group-create producer-grp");
        executeCommand("cluster:group-create consumer-grp");
        System.out.println(executeCommand("cluster:group-set producer-grp " + localNode.getId()));
        System.out.println(executeCommand("cluster:group-set consumer-grp " + node1));
        System.out.println(executeCommand("cluster:group-set consumer-grp " + node2));
        System.out.println(executeCommand("cluster:group-list"));

        System.out.println(executeCommand("cluster:feature-install consumer-grp cellar-sample-camel-consumer"));
        System.out.println(executeCommand("cluster:feature-install producer-grp cellar-sample-camel-producer"));
        Thread.sleep(10000);
        System.out.println(executeCommand("feature:list"));
        System.out.println(executeCommand("bundle:list"));

        System.out.println(executeCommand("cluster:group-list"));
        System.out.println(executeCommand(generateSSH("node2", "bundle:list -t 0")));

        Thread.sleep(10000);
        String output1 = executeCommand(generateSSH("node1", "log:display | grep \"Hallo Cellar\""));
        System.out.println(output1);
        String output2 = executeCommand(generateSSH("node2", "log:display | grep \"Hallo Cellar\""));
        System.out.println(output2);
        assertTrue("Expected at least lines", countOutputEntires(output1) >= 2);
        assertTrue("Expected at least lines", countOutputEntires(output2) >= 2);
    }

    public int countOutputEntires(String output) {
        String[] lines = output.split("\n");
        return lines.length;
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("node1");
            destroyCellarChild("node2");
            unInstallCellar();
        } catch (Exception e) {
            //Ignore
        }
    }

}
