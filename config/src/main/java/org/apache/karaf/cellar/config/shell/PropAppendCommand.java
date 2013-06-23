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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.cellar.core.event.EventType;

import java.util.Map;
import java.util.Properties;
import org.apache.karaf.cellar.core.CellarCluster;

@Command(scope = "cluster", name = "config-propappend", description = "Append to the property value for a configuration PID in a cluster")
public class PropAppendCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "cluster", description = "The cluster name", required = true, multiValued = false)
    String clusterName;

    @Argument(index = 1, name = "pid", description = "The configuration PID", required = true, multiValued = false)
    String pid;

    @Argument(index = 2, name = "key", description = "The property key", required = true, multiValued = false)
    String key;

    @Argument(index = 3, name = "value", description = "The property value", required = true, multiValued = false)
    String value;
    
    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            System.err.println("Cluster " + clusterName + " doesn't exist");
            return null;
        }

        // check if the producer is ON
        if (!cluster.emitsEvents()) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        // check if the config pid is allowed
        if (!isAllowed(cluster, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            System.err.println("Configuration PID " + pid + " is blocked outbound for cluster group " + clusterName);
            return null;
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
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
            event.setSourceCluster(cluster);
            cluster.produce(event);
        } else {
            System.out.println("No configuration found in cluster group " + clusterName);
        }
        return null;
    }
}
