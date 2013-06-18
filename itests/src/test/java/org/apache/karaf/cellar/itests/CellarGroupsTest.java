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

import java.util.Collection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.apache.karaf.cellar.core.CellarCluster;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarGroupsTest extends CellarTestSupport {

    @Test
    //@Ignore
    public void testGroupsWithChildNodes() throws InterruptedException {
        installCellar();
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("cluster:node-list"));
        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodesAllClusters();
        assertTrue("There should be at least 2 cellar nodes running", 2 <= nodes.size());

        System.err.println(executeCommand("cluster:cluster-list"));
        System.err.println(executeCommand("cluster:cluster-set testgroup " + localNode.getId()));
        System.err.println(executeCommand("cluster:cluster-list"));

        Collection<CellarCluster> clusters = clusterManager.getClusters();
        assertEquals("There should be 2 cellar groups", 2, clusters.size());

        System.err.println(executeCommand("cluster:cluster-delete testgroup "));
        System.err.println(executeCommand("cluster:cluster-list"));
        clusters = clusterManager.getClusters();
        assertEquals("There should be a single cellar group", 1, clusters.size());
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("child1");
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
