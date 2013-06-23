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
package org.apache.karaf.cellar.core.control;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.CommandHandler;

import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;

/**
 * Manager cluster command handler.
 */
public class ManageClusterCommandHandler extends CommandHandler<ManageClusterCommand, ManageClusterResult> {
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.managecluster.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public ManageClusterResult execute(ManageClusterCommand command) {

        ManageClusterResult result = new ManageClusterResult(command.getId());
        ManageClusterAction action = command.getAction();

        String targetClusterName = command.getClusterName();
        Node node = clusterManager.getMasterCluster().getLocalNode();

        if (ManageClusterAction.JOIN.equals(action)) {
            joinCluster(targetClusterName);
        } else if (ManageClusterAction.QUIT.equals(action)) {
            quitCluster(targetClusterName);
            if (clusterManager.listLocalClusters().isEmpty()) {
                joinCluster(Configurations.DEFAULT_GROUP_NAME);
            }
        } else if (ManageClusterAction.PURGE.equals(action)) {
            purgeClusters();
            joinCluster(Configurations.DEFAULT_GROUP_NAME);
        } else if (ManageClusterAction.SET.equals(action)) {
            CellarCluster localCluster = clusterManager.listLocalClusters().iterator().next();
            quitCluster(localCluster.getName());
            joinCluster(targetClusterName);
        }

        addClusterListToResult(result);

        return result;
    }

    /**
     * Add the {@link Cluster} list to the result.
     *
     * @param result the result where to add the cluster list.
     */
    public void addClusterListToResult(ManageClusterResult result) {
        Set<CellarCluster> clusters = clusterManager.listAllClusters();

        for (CellarCluster c : clusters) {
            result.getClusters().add(c);
        }
    }

    /**
     * Add {@link Node} to the target {@link CellarCluster}.
     *
     * @param targetClusterName the target cluster name where to add the node.
     */
    public void joinCluster(String targetClusterName) {
        CellarCluster targetCluster = clusterManager.findLocalCluster(targetClusterName);
        if (targetCluster == null) {
            clusterManager.createCluster(targetClusterName);
        } else {
			LOGGER.warn("This node is already a part of cluster: " + targetClusterName);
        }
    }

    /**
     * Leave the target {@link CellarCluster}.
     *
     * @param targetClusterName the target cluster to leave.
     */
    public void quitCluster(String clusterName) {
		CellarCluster targetCluster = clusterManager.findLocalCluster(clusterName);
        if (targetCluster != null) {
            clusterManager.deleteCluster(clusterName);
        } else {
			LOGGER.warn("This node isn't part of cluster: " + clusterName + " and therefore doesn't need to leave it.");
        }
    }

    /**
     * Remove {@link Node} from all {@link CellarCluster}s.
     */
    public void purgeClusters() {
        Set<CellarCluster> clusters = clusterManager.listLocalClusters();
        if ((clusters != null) && (!clusters.isEmpty())) {
            for (CellarCluster cellarCluster : clusters) {
                clusterManager.deleteCluster(cellarCluster);
            }
        } else {
			LOGGER.warn("This node isn't part of any clusters other than the default which cannot be disconected.");
        }
    }

    @Override
    public Class<ManageClusterCommand> getType() {
        return ManageClusterCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }
}
