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

import java.util.Map;
import java.util.Set;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarGroupsTest extends CellarTestSupport {
    private boolean createdChildren = false;

    @Test
    public void testGroupManagerService() throws Exception {
        System.out.println("############################## Starting Test: testGroupManagerService ##################################################" );
        installCellar();
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        assertEquals(1, clusterManager.getClusters().size());
        assertEquals(1, clusterManager.getMasterCluster().listNodes().size());
        assertNotNull(clusterManager.getMasterCluster().getLocalNode());
        assertEquals(clusterManager.getMasterCluster().getLocalNode(), clusterManager.getMasterCluster().listNodes().toArray()[0]);
        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertNotNull(groupManager.getPidForGroup("default"));
        assertTrue(groupManager.listGroupNames().contains("default"));
        assertTrue(groupManager.isLocalGroup("default"));
        assertNotNull(groupManager.findGroupConfigurationByName("default"));
        Map<String, Group> groupMap = groupManager.listGroups();
        assertEquals(1, groupMap.size());
        System.out.println("######################################################################" + groupManager.listGroups());
        assertNotNull(groupMap.get("default"));
        assertEquals(1, groupMap.get("default").getNodes().size());
    }

    @Test
    @Ignore
    public void testGroupsWithChildNodes() throws Exception {
        System.out.println("############################## Starting Test: testGroupsWithChildNodes ##################################################" );
        installCellar();
        createdChildren = true;
        createCellarChild("child1");
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("cluster:node-list"));
        Node localNode = clusterManager.getMasterCluster().getLocalNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 2 cellar nodes running", 2 >= nodes.size());

        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("cluster:group-create testgroup "));
        System.err.println(executeCommand("cluster:group-set testgroup " + localNode.getId()));
        System.err.println(executeCommand("cluster:group-list"));

        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertNotNull(groupManager);
        Set<Group> groups = groupManager.listAllGroups();
        assertEquals("There should be 2 cellar groups", 2, groups.size());

        System.err.println(executeCommand("cluster:group-delete testgroup"));
        System.err.println(executeCommand("cluster:group-list"));
        groups = groupManager.listAllGroups();
        assertEquals("There should be a single cellar group", 1, groups.size());
    }

    @After
    public void tearDown() {
        if (createdChildren) {
            try {
                destroyCellarChild("child1");
                unInstallCellar();
            } catch (Exception ex) {
                //Ignore
            }
        }
    }
}
