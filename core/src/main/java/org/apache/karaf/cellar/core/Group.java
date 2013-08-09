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
package org.apache.karaf.cellar.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Cellar cluster group.
 */
public class Group implements MultiNode {
    private String name;
    private Set<Node> nodes = new HashSet<Node>();

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    /**
     * Removes a node from this group.
     *
     * @param node the node to remove
     * @return true if it was removed (meaning existed), false if not
     */
    @Override
    public boolean removeNode(Node node) {
        return this.nodes.remove(node);
    }

    /**
     * Adds a node to the specified group.
     *
     * @param node the node to add
     */
    @Override
    public void addNode(Node node) {
        this.nodes.add(node);
    }

    /**
     * Checks if a node is part of the specified group.
     *
     * @param node the node to check
     */
    @Override
    public boolean containsNode(Node node) {
        return this.nodes.contains(node);
    }

    @Override
    public Set<Node> getNodes() {
        return nodes;
    }

    @Override
    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Group that = (Group) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Group{" + "name=" + name + ", nodes=" + nodes + '}';
    }
}
