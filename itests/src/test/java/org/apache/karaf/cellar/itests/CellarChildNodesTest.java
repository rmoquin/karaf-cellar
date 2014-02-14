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

import java.security.spec.PSSParameterSpec;
import java.util.Set;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarChildNodesTest extends CellarTestSupport {

    @Test
    public void testClusterWithChildNodes() throws Exception {
        installCellar();
        createCellarChild("child1");
        try {
            Thread.sleep(DELAY_TIMEOUT);
        } catch (InterruptedException e) {
            //Ignore
        }
        System.err.println(executeCommand("cluster:node-list"));
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertNotNull(localNode);
        assertEquals("There should be at least 2 cellar nodes running", 2, nodes.size());
    }

    @After
    public void tearDown() {
        destroyCellarChild("child1");
        unInstallCellar();
    }

}
