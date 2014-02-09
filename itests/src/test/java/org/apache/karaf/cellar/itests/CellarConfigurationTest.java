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

    private static final String TESTPID = "cellar.config.tst";

    @Test
    public void testCellarFeaturesModule() throws Exception {
        installCellar();
        createCellarChild("child1", "child2");

        String node1 = this.getNodeIdOfChild("child1");
        String node2 = this.getNodeIdOfChild("child2");
        System.err.println(executeCommand("instance:list"));

        String properties = super.executeRemoteCommand("child1", "cluster:config-proplist default " + TESTPID);
        assertFalse((properties.contains("myKey")));

        //Test configuration sync - add property
        System.err.println(executeCommand("cluster:config-propset default " + TESTPID + " myKey myValue"));
        Thread.sleep(DELAY_TIMEOUT);
        properties = executeRemoteCommand("child1", "cluster:config-proplist default " + TESTPID);
        System.err.println(properties);
        assertTrue(properties.contains("myKey") && properties.contains("myValue"));

        //Test configuration sync - remove property
        System.err.println(executeCommand("cluster:config-propdel default " + TESTPID + " myKey"));
        Thread.sleep(DELAY_TIMEOUT);
        properties = executeRemoteCommand("child1", "cluster:config-proplist default " + TESTPID);
        System.err.println(properties);
        assertFalse(properties.contains("myKey"));

        //Test configuration sync - add property - join later
        System.err.println(executeCommand("cluster:group-create new-grp"));
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:group-set new-grp " + node1));
        Thread.sleep(DELAY_TIMEOUT);
        executeRemoteCommand("child1", "cluster:config-propset new-grp " + TESTPID + " myKey2 myValue2");
        Thread.sleep(DELAY_TIMEOUT);
        properties = executeRemoteCommand("child1", "cluster:config-proplist new-grp " + TESTPID);
        System.err.println(properties);
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(executeCommand("cluster:group-set new-grp " + node2));
        properties = executeRemoteCommand("child2", "cluster:config-proplist new-grp " + TESTPID);
        Thread.sleep(DELAY_TIMEOUT);
        System.err.println(properties);
        assertTrue(properties.contains("myKey2") && properties.contains("myValue2"));
    }

    @After
    public void tearDown() {
        destroyCellarChild("child2");
        destroyCellarChild("child1");
        unInstallCellar();
    }

}
