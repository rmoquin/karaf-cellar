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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.config.ClusterConfigurationEvent;
import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.management.CellarConfigMBean;
import org.osgi.service.cm.ConfigurationEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;

/**
 * Implementation of the Cellar Config MBean.
 */
public class CellarConfigMBeanImpl extends StandardMBean implements CellarConfigMBean {

    private ClusterManager clusterManager;

    public CellarConfigMBeanImpl() throws NotCompliantMBeanException {
        super(CellarConfigMBean.class);
    }

    @Override
    public List<String> listConfig(String clusterName) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster " + clusterName + " doesn't exist");
        }

        List<String> result = new ArrayList<String>();

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
        for (String pid : clusterConfigurations.keySet()) {
            result.add(pid);
        }

        return result;
    }

    @Override
    public void deleteConfig(String clusterName, String pid) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster " + clusterName + " doesn't exist");
        }

        // check if the producer is ON
        if (!cluster.emitsEvents()) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the PID is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            throw new IllegalStateException("Configuration PID " + pid + " is blocked outbound for cluster group " + clusterName);
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
        if (clusterConfigurations != null) {
            // update the cluster group
            Properties properties = clusterConfigurations.remove(pid);

            // broadcast the cluster event
            ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
            event.setSourceCluster(cluster);
            event.setType(ConfigurationEvent.CM_DELETED);
            cluster.produce(event);
        } else {
            throw new IllegalArgumentException("No configuration found in cluster group " + clusterName);
        }
    }

    @Override
    public TabularData listProperties(String clusterName, String pid) throws Exception {

        CompositeType compositeType = new CompositeType("Property", "Cellar Config Property",
                new String[]{"key", "value"},
                new String[]{"Property key", "Property value"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Properties", "Table of all properties in the configuration PID",
                compositeType, new String[]{"key"});
        TabularData table = new TabularDataSupport(tableType);
        
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
        Properties clusterProperties = clusterConfigurations.get(pid);
        if (clusterProperties != null) {
            Enumeration propertyNames = clusterProperties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = (String) propertyNames.nextElement();
                String value = (String) clusterProperties.get(key);
                CompositeDataSupport data = new CompositeDataSupport(compositeType,
                        new String[]{"key", "value"},
                        new String[]{key, value});
                table.put(data);
            }
        }
        return table;
    }

    @Override
    public void setProperty(String clusterName, String pid, String key, String value) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        // check if the producer is ON
        if (!cluster.emitsEvents()) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the PID is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            throw new IllegalStateException("Configuration PID " + pid + " is blocked outbound for cluster group " + clusterName);
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
        if (clusterConfigurations != null) {
            // update the cluster group
            Properties clusterProperties = clusterConfigurations.get(pid);
            if (clusterProperties == null) {
                clusterProperties = new Properties();
            }
            clusterProperties.put(key, value);
            clusterConfigurations.put(pid, clusterProperties);

            // broadcast the cluster event
            ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
            event.setSourceCluster(cluster);
            cluster.produce(event);
        } else {
            throw new IllegalArgumentException("No configuration found in cluster group " + clusterName);
        }
    }

    @Override
    public void appendProperty(String groupName, String pid, String key, String value) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(groupName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is on
        if (!cluster.emitsEvents()) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the pid is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            throw new IllegalStateException("Configuration PID " + pid + " is blocked outbound for cluster group " + groupName);
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (clusterConfigurations != null) {
            // update the cluster group
            Properties clusterProperties = clusterConfigurations.get(pid);
            if (clusterProperties == null) {
                clusterProperties = new Properties();
            }
            Object currentValue = clusterProperties.get(key);
            if (currentValue == null) {
                clusterProperties.put(key, value);
            } else if (currentValue instanceof String) {
                clusterProperties.put(key, currentValue + value);
            } else {
                throw new IllegalStateException("Append failed: current value is not a String");
            }
            clusterConfigurations.put(pid, clusterProperties);

            // broadcast the cluster event
            ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
            event.setSourceCluster(cluster);
            cluster.produce(event);
        } else {
            throw new IllegalArgumentException("No configuration found in cluster group " + groupName);
        }
    }

    @Override
    public void deleteProperty(String clusterName, String pid, String key) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        // check if the event producer is ON
        if (!cluster.emitsEvents()) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the pid is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.CATEGORY, pid, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Configuration PID " + pid + " is blocked outbound for cluster group " + clusterName);
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + clusterName);
        if (clusterConfigurations != null) {
            // update the cluster group
            Properties clusterDictionary = clusterConfigurations.get(pid);
            if (clusterDictionary != null) {
                clusterDictionary.remove(key);
                clusterConfigurations.put(pid, clusterDictionary);
                // broadcast the cluster event
                ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
                event.setSourceCluster(cluster);
                cluster.produce(event);
            }
        } else {
            throw new IllegalArgumentException("No configuration found in cluster group " + clusterName);
        }
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }
}
