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
package org.apache.karaf.cellar.core.command;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.karaf.cellar.core.Node;

/**
 *
 * @author rmoquin
 */
public interface DistributedExecutionContext<T, R> {
    /**
     * @return the name
     */
    String getName();

    public Map<Node, Future<R>> execute(T command, Set<Node> destinations);

    public Map<Node, Future<R>> execute(T command, Node destination);

    public Map<Node, R> executeAndWait(T command, Set<Node> destinations);

    public Map<Node, R> executeAndWait(T command, Node destination);

    public void executeAsync(T command, Set<Node> destinations, DistributedMultiCallback callback);

    public void executeAsync(T command, Node destination, DistributedCallback<R> callback);

    /**
     * @param timeoutSeconds the timeoutSeconds to set
     */
    void setTimeoutSeconds(int timeoutSeconds);

    /**
     * @return the timeoutSeconds
     */
    int getTimeoutSeconds();
}
