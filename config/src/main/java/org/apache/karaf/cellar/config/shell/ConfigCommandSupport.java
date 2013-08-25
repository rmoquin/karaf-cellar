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

import java.util.Set;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

/**
 * Abstract cluster config command support.
 */
public abstract class ConfigCommandSupport extends CellarCommandSupport {
    CellarSupport support = new CellarSupport();
    
    /**
     * Check if a configuration is allowed.
     *
     * @param pid the configuration PID.
     * @return true if the cluster event type is allowed, false else.
     */
    public boolean isAllowed(String pid, Set<String> whitelist, Set<String> blacklist) {
        return support.isAllowed(pid, whitelist, blacklist);
    }
}
