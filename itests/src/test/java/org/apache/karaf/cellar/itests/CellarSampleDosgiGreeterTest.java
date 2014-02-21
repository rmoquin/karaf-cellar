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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarSampleDosgiGreeterTest extends CellarTestSupport {

    @Test
    public void testDosgiGreeter() throws Exception {
        installCellar();
        createCellarChild("node1", "node2");
        System.err.println(executeCommand("instance:list"));
        System.err.println(executeCommand("cluster:node-list"));
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        String node1 = this.getNodeIdOfChild("node1");
        String node2 = this.getNodeIdOfChild("node2");

        executeCommand("cluster:group-create client-grp");
        executeCommand("cluster:group-create service-grp");
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("cluster:group-set client-grp " + localNode.getId()));
        System.err.println(executeCommand("cluster:group-set service-grp " + node1));
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:feature-url-add client-grp mvn:org.apache.karaf.cellar.samples/dosgi-greeter/" + CELLAR_VERSION + "/xml/features"));
        System.err.println(executeCommand("cluster:feature-url-add service-grp mvn:org.apache.karaf.cellar.samples/dosgi-greeter/" + CELLAR_VERSION + "/xml/features"));
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:feature-install client-grp greeter-client"));
        System.err.println(executeCommand("cluster:feature-install service-grp greeter-service"));
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:service-list"));
        String greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
        System.err.println(greetOutput);
        assertEquals("Expected 10 greets", 10, countGreetsFromNode(greetOutput, node1));

        //TODO I'm not sure if this portion of the test is realistc since the lient reeernce was already set
//        System.err.println(executeCommand("cluster:group-set service-grp " + node2));
//        Thread.sleep(DELAY_TIMEOUT);
//        System.err.println(executeCommand("cluster:group-list"));
//        System.err.println(executeCommand("cluster:service-list"));
//        greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
//        System.err.println(greetOutput);
//        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node1));
//        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node2));
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
        destroyCellarChild("node1");
        destroyCellarChild("node2");
        super.unInstallCellar();
    }

}
