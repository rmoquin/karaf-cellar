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
import static org.apache.karaf.cellar.itests.CellarTestSupport.SERVICE_TIMEOUT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CellarConfigurationTest extends CellarTestSupport {

    private static final String TESTPID = "org.apache.karaf.cellar.tst";

    @Test
    public void testCellarFeaturesModule() throws Exception {
        installCellar();
        createCellarChild("child1", "child2");

        final ClusterManager manager = this.getOsgiService(ClusterManager.class, SERVICE_TIMEOUT);
        String node1 = manager.findNodeByName("child1").getId();
        String node2 = manager.findNodeByName("child2").getId();
        System.err.println(executeCommand("instance:list"));

        String properties = super.executeRemoteCommand("child1", "cluster:config-proplist " + TESTPID);
        System.err.println(properties);
        assertFalse((properties.contains("myKey")));

        //Test configuration sync - add property
        System.err.println(executeCommand("cluster:config-propset --pid " + TESTPID + " myKey myValue"));
        Thread.sleep(DELAY_TIMEOUT);
        properties = executeRemoteCommand("child1", "cluster:config-proplist  " + TESTPID);
        System.err.println(properties);
        assertTrue(properties.contains("myKey = myValue"));

        //Test configuration sync - remove property
        System.err.println(executeCommand("cluster:config-propdel --pid " + TESTPID + " myKey"));
        Thread.sleep(DELAY_TIMEOUT);
        properties = executeRemoteCommand("child1", "cluster:config-proplist " + TESTPID);
        System.err.println(properties);
        assertFalse(properties.contains("myKey"));

        //Test configuration sync - add property - join later
        System.err.println(executeCommand("cluster:group-set new-grp " + node1));
        Thread.sleep(DELAY_TIMEOUT);
        executeRemoteCommand("child1", "cluster:config-propset --pid " + TESTPID + " myKey2 myValue2");

        properties = executeRemoteCommand("child1", "cluster:config-proplist " + TESTPID);
        System.err.println(properties);

        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:group-set new-grp " + node2));
        properties = executeRemoteCommand("child2", "cluster:config-proplist --pid " + TESTPID);
        System.err.println(properties);
        assertTrue(properties.contains("myKey2 = myValue2"));
    }

    @After
    public void tearDown() {
        destroyCellarChild("child1");
        destroyCellarChild("child2");
        unInstallCellar();
    }

}
