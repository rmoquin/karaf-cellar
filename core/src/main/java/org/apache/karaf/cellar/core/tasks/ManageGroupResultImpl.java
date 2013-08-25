/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core.tasks;

import java.util.HashSet;
import java.util.Set;
import org.apache.karaf.cellar.core.Group;

/**
 *
 * @author rmoquin
 */
public class ManageGroupResultImpl implements ManageGroupResult {
    private boolean successful = true;
    private Throwable throwable;
    private Set<Group> groups = new HashSet<Group>();

    public ManageGroupResultImpl() {
    }
    
    @Override
    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }
    
    /**
     * @param throwable the throwable to set
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    /**
     * @param successful the successful to set
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
