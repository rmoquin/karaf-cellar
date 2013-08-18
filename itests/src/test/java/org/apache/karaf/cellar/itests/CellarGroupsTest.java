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
import static org.apache.karaf.cellar.itests.CellarTestSupport.COMMAND_TIMEOUT;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarGroupsTest extends CellarTestSupport {
    private boolean createdChildren = false;

    @Test
    //@Ignore
    public void testGroupManagerService() throws Exception {
        installCellar();
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        assertEquals(1, clusterManager.getClusters().size());
        assertEquals(1, clusterManager.getMasterCluster().listNodes().size());
        assertNotNull(clusterManager.getMasterCluster().getLocalNode());
        assertEquals(clusterManager.getMasterCluster().getLocalNode(), clusterManager.getMasterCluster().listNodes().toArray()[0]);
        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertTrue(groupManager.getJoinedGroupNames().contains("default"));
        assertTrue(groupManager.isLocalGroup("default"));
        assertNotNull(groupManager.findGroupConfigurationByName("default"));
        Map<String, Group> groupMap = groupManager.listGroups();
        assertEquals(1, groupMap.size());
        assertNotNull(groupMap.containsKey("default"));
        assertEquals(1, groupMap.get("default").getNodes().size());
    }

    @Test
    public void testGroupsWithChildNodes() throws Exception {
        installCellar();
        createdChildren = true;
        createCellarChild("child1");
        if (!waitForInstanceToCluster(2)) {
            throw new Exception("Failed waiting for second node to connect to cluster..");
        }
        String child1Id = getNodeIdOfChild("child1");
        System.out.println(executeCommand("cluster:group-list"));
        System.out.println(executeCommand("cluster:group-create testgroup"));
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:group-set testgroup " + child1Id));
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:group-list"));

        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertNotNull(groupManager);
        Set<Group> groups = groupManager.listAllGroups();
        assertEquals("There should be 2 cellar groups", 2, groups.size());

        System.out.println(executeCommand("cluster:group-delete testgroup"));
        Thread.sleep(DELAY_TIMEOUT);
        System.out.println(executeCommand("cluster:group-list"));
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
