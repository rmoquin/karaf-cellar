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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.shell.commands.Command;
import org.osgi.framework.ServiceReference;

import java.util.Set;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

@Command(scope = "cluster", name = "sync", description = "Force the call of all cluster synchronizers available")
public class SyncCommand extends CellarCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        Set<Group> localGroups = groupManager.listLocalGroups();
        for (Group group : localGroups) {
            System.out.println("Synchronizing cluster group " + group.getName());
            ServiceReference[] serviceReferences = getBundleContext().getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
            if (serviceReferences != null && serviceReferences.length > 0) {
                for (ServiceReference ref : serviceReferences) {
                    try {
                        Synchronizer synchronizer = (Synchronizer) getBundleContext().getService(ref);
                        if (synchronizer.isSyncEnabled(group)) {
                            System.out.print("    sync " + synchronizer.getClass() + " ...");
                            synchronizer.pull(group);
                            synchronizer.push(group);
                            System.out.println("OK");
                        }
                    } finally {
                        getBundleContext().ungetService(ref);
                    }
                }
            }
        }
        return null;
    }
}
