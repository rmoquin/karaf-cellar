/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;
import org.apache.karaf.cellar.core.event.EventType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;


import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

public class CellarSupportTest {
    ConfigurationAdmin configurationAdmin = createMock(ConfigurationAdmin.class);
    SynchronizationConfiguration configuration = new SynchronizationConfiguration() {
        Dictionary<String, Object> properties;

        @Override
        public void save() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setProperties(Dictionary<String, Object> properties) {
            this.properties = properties;
        }

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public void setProperty(String name, Object value) {
            this.properties.put(name, value);
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return properties;
        }
    };
    Properties props = new Properties();

    @Before
    public void setUp() throws Exception {
        InputStream is = getClass().getResourceAsStream("synchronization.cfg");
        props.load(is);
        is.close();
        Dictionary propsDictionary = props;
        configuration.setProperties(propsDictionary);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIsAllowed() {
        CellarSupport support = new CellarSupport();
        support.setSynchronizationConfiguration(configuration);

        Boolean expectedResult = false;
        Boolean result = support.isAllowed("default", "config", "org.apache.karaf.shell", EventType.INBOUND);
        assertEquals("Shell should not be allowed", expectedResult, result);

        expectedResult = true;
        result = support.isAllowed("default", "config", "org.apache.karaf.cellar.group", EventType.INBOUND);
        assertEquals("Group config should be allowed", expectedResult, result);

        expectedResult = false;
        result = support.isAllowed("default", "config", "org.apache.karaf.cellar.node", EventType.INBOUND);
        assertEquals("Node config should be allowed", expectedResult, result);

        expectedResult = false;
        result = support.isAllowed("default", "config", "org.apache.karaf.cellar.instance", EventType.INBOUND);
        assertEquals("Instance config should be allowed", expectedResult, result);
    }
}
