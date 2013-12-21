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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarFeaturesTest extends CellarTestSupport {

    private static final String UNINSTALLED = "[uninstalled]";
    private static final String INSTALLED = "[installed  ]";

    @Test
    public void testCellarFeaturesModule() throws Exception {
        installCellar();
        createCellarChild("child1");
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.out.println(executeCommand("instance:list"));

        String eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install
        System.out.println(executeCommand("cluster:feature-install default eventadmin"));
        Thread.sleep(DELAY_TIMEOUT);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        //Test feature sync - uninstall
        System.out.println(executeCommand("feature:uninstall eventadmin"));
        Thread.sleep(DELAY_TIMEOUT);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install - before a node joins
        System.out.println(executeCommand("cluster:feature-install default eventadmin"));
        Thread.sleep(DELAY_TIMEOUT);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        //Test feature command - uninstall
        System.out.println(executeCommand("cluster:feature-uninstall default eventadmin"));
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install - before a node joins
        System.out.println(executeCommand("cluster:feature-install testgroup eventadmin"));
        System.out.println(executeCommand("cluster:group-set testgroup " + getNodeIdOfChild("child1")));
        Thread.sleep(DELAY_TIMEOUT);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.out.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        System.out.println(executeCommand("cluster:node-list"));
        assertNotNull(localNode);
        assertTrue("There should be at least 2 cellar nodes running", 2 <= nodes.size());
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
