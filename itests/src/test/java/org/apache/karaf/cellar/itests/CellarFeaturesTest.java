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
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarFeaturesTest extends CellarTestSupport {
    private static final String UNINSTALLED = "[uninstalled]";
    private static final String INSTALLED = "[installed  ]";

    @Test
    //@Ignore
    public void testCellarFeaturesModule() throws InterruptedException {
        installCellar();
        System.err.println(executeCommand("feature:list"));
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        //Test feature sync - install
        System.err.println(executeCommand("feature:install eventadmin"));
        Thread.sleep(5000);
        String eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        System.err.println(executeCommand("instance:list"));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install
        System.err.println(executeCommand("cluster:feature-install default eventadmin"));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        //Test feature sync - uninstall
        System.err.println(executeCommand("feature:uninstall eventadmin"));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install - before a node joins
        System.err.println(executeCommand("cluster:feature-install testgroup eventadmin"));
        System.err.println(executeCommand("cluster:group-set testgroup " + getNodeIdOfChild("child1")));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        //Test feature command - uninstall
        System.err.println(executeCommand("cluster:feature-uninstall default eventadmin"));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install - before a node joins
        System.err.println(executeCommand("cluster:feature-install testgroup eventadmin"));
        System.err.println(executeCommand("cluster:group-set testgroup " + getNodeIdOfChild("child1")));
        Thread.sleep(5000);
        eventadminFeatureStatus = executeCommand("instance:connect -u karaf -p karaf child1 feature:list | grep eventadmin");
        System.err.println(eventadminFeatureStatus);
        assertTrue(eventadminFeatureStatus.startsWith(INSTALLED));

        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        System.err.println(executeCommand("cluster:node-list"));
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
