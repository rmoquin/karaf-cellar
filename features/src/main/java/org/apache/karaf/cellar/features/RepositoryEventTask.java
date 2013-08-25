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
package org.apache.karaf.cellar.features;

import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import org.apache.karaf.cellar.core.command.DistributedTask;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

/**
 * Handler for cluster features repository event.
 */
public class RepositoryEventTask extends DistributedTask<RespositoryEventResponse> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RepositoryEventTask.class);
    private String id;
    private RepositoryEvent.EventType type;
    private Boolean install;
    private Boolean uninstall;

    public RepositoryEventTask() {
    }

    public RepositoryEventTask(String id, RepositoryEvent.EventType type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Handle cluster features repository event.
     *
     * @return
     */
    @Override
    protected RespositoryEventResponse execute() throws Exception {

        RespositoryEventResponse result = new RespositoryEventResponse();
        try {
            FeaturesService featuresService = super.getService(FeaturesService.class);
            // TODO check if isAllowed
            boolean registered = false;
            Repository[] localRepositories = featuresService.listRepositories();
            for (Repository localRepository : localRepositories) {
                if (localRepository.getURI().toString().equals(id)) {
                    registered = true;
                    break;
                }
            }
            if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                if (!registered) {
                    LOGGER.debug("CELLAR FEATURES: adding repository URI {}", id);
                    featuresService.addRepository(new URI(id), install);
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is already registered locally");
                }
            } else {
                if (registered) {
                    LOGGER.debug("CELLAR FEATURES: removing repository URI {}", id);
                    featuresService.removeRepository(new URI(id), uninstall);
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is not registered locally");
                }
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR FEATURES: failed to handle cluster feature event", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    public RepositoryEvent.EventType getType() {
        return type;
    }

    public Boolean getInstall() {
        return install;
    }

    public void setInstall(Boolean install) {
        this.install = install;
    }

    public Boolean getUninstall() {
        return uninstall;
    }

    public void setUninstall(Boolean uninstall) {
        this.uninstall = uninstall;
    }
}
