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
package org.apache.karaf.cellar.obr.shell;

import java.util.List;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

/**
 * Generic cluster OBR shell command support.
 */
public abstract class ObrCommandSupport extends CellarCommandSupport {

    protected RepositoryAdmin obrService;

    public RepositoryAdmin getObrService() {
        return this.obrService;
    }

    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

    /**
     * Check if an OBR cluster event is allowed.
     *
     * @param id the event ID.
     * @param whitelist
     * @param blacklist
     * @return in case of check failure.
     */
    public boolean isAllowed(String id, List<String> whitelist, List<String> blacklist) {
        CellarSupport support = new CellarSupport();
        return support.isAllowed(id, whitelist, blacklist);
    }

    @Override
    public abstract Object doExecute() throws Exception;


}
