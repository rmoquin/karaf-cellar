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
package org.apache.karaf.cellar.obr.management.internal;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.obr.ClusterObrBundleEvent;
import org.apache.karaf.cellar.obr.ClusterObrUrlEvent;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleInfo;
import org.apache.karaf.cellar.obr.management.CellarOBRMBean;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the Cellar OBR MBean.
 */
public class CellarOBRMBeanImpl extends StandardMBean implements CellarOBRMBean {

    private ClusterManager clusterManager;
    private EventProducer eventProducer;
    private ConfigurationAdmin configurationAdmin;
    private RepositoryAdmin obrService;

    public CellarOBRMBeanImpl() throws NotCompliantMBeanException {
        super(CellarOBRMBean.class);
    }

    @Override
    public List<String> listUrls(String clusterName) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        List<String> result = new ArrayList<String>();
        Set<String> clusterUrls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        for (String url : clusterUrls) {
            result.add(url);
        }
        return result;
    }

    @Override
    public TabularData listBundles(String clusterName) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        CompositeType compositeType = new CompositeType("OBR Bundle", "Bundles available in the OBR service",
                new String[]{ "name", "symbolic", "version"},
                new String[]{ "Name of the bundle", "Symbolic name of the bundle", "Version of the bundle" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });
        TabularType tableType = new TabularType("OBR Bundles", "Table of all bundles available in the OBR service", compositeType,
                new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

            Set<ObrBundleInfo> clusterBundles = cluster.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
            for (ObrBundleInfo info : clusterBundles) {
                CompositeData data = new CompositeDataSupport(compositeType,
                        new String[]{ "name", "symbolic", "version" },
                        new Object[]{ info.getPresentationName(), info.getSymbolicName(), info.getVersion() });
                table.put(data);
            }
        return table;
    }

    @Override
    public void addUrl(String clusterName, String url) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the URL is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.URLS_CONFIG_CATEGORY, url, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR URL " + url + " is blocked outbound for cluster group " + clusterName);
        }

        // update OBR URLs in the cluster group
        Set<String> clusterUrls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        clusterUrls.add(url);
        // update OBR bundles in the cluster group
        Set<ObrBundleInfo> clusterBundles = cluster.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        synchronized (obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                clusterBundles.add(info);
            }
            obrService.removeRepository(url);
        }

        // broadcast a cluster event
        ClusterObrUrlEvent event = new ClusterObrUrlEvent(url, Constants.URL_ADD_EVENT_TYPE);
        event.setForce(true);
        event.setSourceCluster(cluster);
        eventProducer.produce(event);
    }

    @Override
    public void removeUrl(String clusterName, String url) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster " + clusterName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the URL is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.URLS_CONFIG_CATEGORY, url, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR URL " + url + " is blocked outbound for cluster group " + clusterName);
        }

        // update the OBR URLs in the cluster group
        Set<String> clusterUrls = cluster.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        clusterUrls.remove(url);
        // update the OBR bundles in the cluster group
        Set<ObrBundleInfo> clusterBundles = cluster.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + clusterName);
        synchronized (obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                clusterBundles.remove(info);
            }
            obrService.removeRepository(url);
        }

        // broadcast a cluster event
        ClusterObrUrlEvent event = new ClusterObrUrlEvent(url, Constants.URL_REMOVE_EVENT_TYPE);
        event.setSourceCluster(cluster);
        eventProducer.produce(event);
    }

    @Override
    public void deploy(String clusterName, String bundleId) throws Exception {
        // check if the group exists
        CellarCluster cluster = clusterManager.findClusterByName(clusterName);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster group " + clusterName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the bundle ID is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        if (!support.isAllowed(cluster.getName(), Constants.BUNDLES_CONFIG_CATEGORY, bundleId, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR bundle " + bundleId + " is blocked outbound for cluster group " + clusterName);
        }

        // broadcast a cluster event
        int type = 0;
        ClusterObrBundleEvent event = new ClusterObrBundleEvent(bundleId, type);
        event.setForce(true);
        event.setSourceCluster(cluster);
        eventProducer.produce(event);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public RepositoryAdmin getObrService() {
        return obrService;
    }

    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
