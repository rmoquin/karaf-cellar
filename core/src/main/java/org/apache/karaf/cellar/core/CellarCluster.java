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
package org.apache.karaf.cellar.core;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author rmoquin
 */
public interface CellarCluster {
    /**
     * Get a map in the cluster.
     *
     * @param mapName the map name in the cluster.
     * @return the map in the cluster.
     */
    public Map getMap(String mapName);

    /**
     * Get a list in the cluster.
     *
     * @param listName the list name in the cluster.
     * @return the list in the cluster.
     */
    public List getList(String listName);

    /**
     * Get a set in the cluster.
     *
     * @param setName the set name in the cluster.
     * @return the set in the cluster.
     */
    public Set getSet(String setName);

    /**
     * Get the nodes in the cluster.
     *
     * @return the set of nodes in the cluster.
     */
    public Set<Node> listNodes();

    /**
     * Get the nodes with a given ID.
     *
     * @param ids the collection of ID to look for.
     * @return the set of nodes.
     */
    public Set<Node> listNodes(Collection<String> ids);

    /**
     * Get a node identified by a given ID.
     *
     * @param id the id of the node to look for.
     * @return the node.
     */
    public Node findNodeById(String id);

    /**
     * Get a node identified by a given name.
     *
     * @param name the name of the node to look for.
     * @return the node.
     */
    public Node findNodeByName(String name);

    /**
     * Returns whether or not this cluster contains a node with the specified id.
     *
     * @param id the node ID.
     * @return true if there is a node with that id.
     */
    public boolean hasNodeWithId(String id);

    /**
     * Returns whether or not this cluster contains a node with the specified name.
     *
     * @param name the node name.
     * @return true if there is a node with that name.
     */
    boolean hasNodeWithName(String name);

    /**
     * Get the local node.
     *
     * @return the local node.
     */
    public Node getLocalNode();

    /**
     * Generate an unique ID across the cluster.
     *
     * @return a unique ID across the cluster.
     */
    public String generateId();

    /**
     * @return the cluster Name
     */
    public String getName();

    void shutdown();
}
