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
package org.apache.karaf.cellar.obr;

import java.util.Set;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.GroupConfiguration;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.DistributedTask;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.obr.Constants.UrlEventTypes;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster OBR URL event.
 */
public class ClusterObrUrlEvent extends DistributedTask<ClusterObrEventResponse> {

    private final String url;
    private final UrlEventTypes type;
    private RepositoryAdmin obrService;

    public ClusterObrUrlEvent(String url, UrlEventTypes type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return this.url;
    }

    public UrlEventTypes getType() {
        return this.type;
    }

    @Override
    protected ClusterObrEventResponse execute() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
