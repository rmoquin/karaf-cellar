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
package org.apache.karaf.cellar.config.shell;

import org.apache.karaf.cellar.config.ClusterConfigurationEvent;
import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.cellar.core.event.EventType;

import java.util.Map;
import java.util.Properties;

@Command(scope = "cluster", name = "config-propappend", description = "Append to the property value for a configuration PID in a cluster group")
public class PropAppendCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID", required = true, multiValued = false)
    String pid;

    @Argument(index = 2, name = "key", description = "The property key", required = true, multiValued = false)
    String key;

    @Argument(index = 3, name = "value", description = "The property value", required = true, multiValued = false)
    String value;

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        // check if the config pid is allowed
        if (!isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            System.err.println("Configuration PID " + pid + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (clusterConfigurations != null) {
            // update the configurations in the cluster group
            Properties properties = clusterConfigurations.get(pid);
            if (properties == null) {
                properties = new Properties();
            }
            Object currentValue = properties.get(key);
            if (currentValue == null) {
                properties.put(key, value);
            } else if (currentValue instanceof String) {
                properties.put(key, currentValue + value);
            } else {
                System.err.println("Append failed: current value is not a String");
                return null;
            }
            clusterConfigurations.put(pid, properties);

            // broadcast the cluster event
            ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
            event.setSourceGroup(group);
            eventProducer.produce(event);
        } else {
            System.out.println("No configuration found in cluster group " + groupName);
        }
        return null;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
