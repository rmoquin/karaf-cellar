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

import java.util.Set;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.CommandHandler;

import org.apache.karaf.cellar.core.CellarCluster;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Manager cluster command handler.
 */
public class ManageClusterCommandHandler extends CommandHandler<ManageClusterCommand, ManageClusterResult> {
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.managecluster.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private ConfigurationAdmin configAdmin;

    @Override
    public ManageClusterResult execute(ManageClusterCommand command) {

        ManageClusterResult result = new ManageClusterResult(command.getId());
        ManageClusterAction action = command.getAction();

        String targetClusterName = command.getClusterName();

        if (ManageClusterAction.JOIN.equals(action)) {
            joinCluster(targetClusterName);
        } else if (ManageClusterAction.LEAVE.equals(action)) {
            leaveCluster(targetClusterName);
        } else if (ManageClusterAction.LEAVE_ALL.equals(action)) {
            leaveAllClusters();
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
        //TODO Figure out what this means.
        Set<CellarCluster> clusters = clusterManager.getClusters();

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
        //TODO Figure out what this means.
        Node node = getClusterManager().getFirstCluster().getLocalNode();
        CellarCluster targetCluster = clusterManager.findClusterByName(targetClusterName);
        if (targetCluster == null) {
            clusterManager.joinCluster(targetClusterName);
        } else if (!targetCluster.listNodes().contains(node)) {
            targetCluster.listNodes().add(node);
            clusterManager.getClusters().add(targetCluster);
            clusterManager.joinCluster(targetClusterName);
        }
    }

    /**
     * Leave the target {@link CellarCluster}.
     *
     * @param targetClusterName the target cluster to leave.
     */
    public void leaveCluster(String targetClusterName) {
        //TODO Figure out out how to remove the cluster config
        CellarCluster cluster = getClusterManager().findClusterByName(targetClusterName);

    }

    /**
     * Remove {@link Node} from all {@link CellarCluster}s.
     */
    public void leaveAllClusters() {
        Set<CellarCluster> clusters = getClusterManager().getClusters();
        //TODO Figure out out how to remove the cluster configs.
    }

    @Override
    public Class<ManageClusterCommand> getType() {
        return ManageClusterCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

    /**
     * @return the configAdmin
     */
    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    /**
     * @param configAdmin the configAdmin to set
     */
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}
