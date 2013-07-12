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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import org.apache.karaf.cellar.core.Node;

/**
 * Cellar membership listener.
 */
public class CellarMembershipListener implements MembershipListener {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(CellarMembershipListener.class);
    private HazelcastCluster masterCluster;
    private GroupManager groupManager;
    private List<? extends Synchronizer> synchronizers;

    public void init() {
        this.masterCluster.addMembershipListener(this);
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        try {
            Node localNode = this.masterCluster.getLocalNode();
            if (localNode.getId().equals(member.getUuid()) && synchronizers != null && !synchronizers.isEmpty()) {
                Set<Group> groups = groupManager.listLocalGroups();
                for (Group group : groups) {
                    for (Synchronizer synchronizer : synchronizers) {
                        if (synchronizer.isSyncEnabled(group)) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Synchronizing local node via pull from group: " + group.getName());
                            }
                            synchronizer.pull(group);
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Synchronizing local node via push to group: " + group.getName());
                            }
                            synchronizer.push(group);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR HAZELCAST: error while calling memberAdded", e);
        }
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        // nothing to do
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public List<? extends Synchronizer> getSynchronizers() {
        return synchronizers;
    }

    public void setSynchronizers(List<? extends Synchronizer> synchronizers) {
        this.synchronizers = synchronizers;
    }

    /**
     * @return the masterCluster
     */
    public HazelcastCluster getMasterCluster() {
        return masterCluster;
    }

    /**
     * @param masterCluster the masterCluster to set
     */
    public void setMasterCluster(HazelcastCluster masterCluster) {
        this.masterCluster = masterCluster;
    }
}
