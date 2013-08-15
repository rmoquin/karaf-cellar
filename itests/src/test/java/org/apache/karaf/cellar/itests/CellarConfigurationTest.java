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

    private static final String TESTPID = "org.apache.karaf.cellar.tst";

    @Test
    //@Ignore
    public void testCellarFeaturesModule() throws Exception {
        installCellar();
        createCellarChild("child1");
        if (!waitForInstanceToCluster(2)) {
            throw new Exception("Failed waiting for node to connect to cluster..");
        }
        createCellarChild("child2");
        if (!waitForInstanceToCluster(3)) {
            throw new Exception("Failed waiting for node to connect to cluster..");
        }

        String node1 = getNodeIdOfChild("child1");
        String node2 = getNodeIdOfChild("child2");

        String properties = executeCommand(generateSSH("child1", "config:proplist --pid " + TESTPID));
        System.out.println(properties);
        assertFalse((properties.contains("myKey")));

        //Test configuration sync - add property
        System.out.println(executeCommand("config:propset --pid " + TESTPID + " myKey myValue"));
        Thread.sleep(5000);
        properties = executeCommand(generateSSH("child1", "config:proplist --pid " + TESTPID));
        System.out.println(properties);
        assertTrue(properties.contains("myKey = myValue"));

        //Test configuration sync - remove property
        System.out.println(executeCommand("config:propdel --pid " + TESTPID + " myKey"));
        Thread.sleep(5000);
        properties = executeCommand(generateSSH("child1", "config:proplist --pid " + TESTPID));
        System.out.println(properties);
        assertFalse(properties.contains("myKey"));

        //Test configuration sync - add property - join later
        System.out.println(executeCommand("cluster:group-set new-grp " + node1));
        Thread.sleep(5000);
        System.out.println(executeCommand(generateSSH("child1", "config:propset --pid " + TESTPID + " myKey2 myValue2")));
        properties = executeCommand(generateSSH("child1", "config:proplist --pid " + TESTPID));
        Thread.sleep(5000);
        System.out.println(executeCommand("cluster:group-set new-grp " + node2));
        properties = executeCommand(generateSSH("child2", "config:proplist --pid " + TESTPID));
        System.out.println(properties);
        assertTrue(properties.contains("myKey2 = myValue2"));
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("child1");
            destroyCellarChild("child2");
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
