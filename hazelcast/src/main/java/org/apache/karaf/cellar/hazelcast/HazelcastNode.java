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
import org.apache.karaf.cellar.core.Node;

/**
 * Cluster node powered by Hazelcast.
 */
public class HazelcastNode implements Node {

    private String id;
    private String name;
    private String host;
    private int port;

    public HazelcastNode() {
    }

    public HazelcastNode(Member member) {
        this.init(member);
    }

    public void init(Member hzMember) {
        this.id = hzMember.getUuid();
        this.host = hzMember.getInetSocketAddress().getHostString();
        this.port = hzMember.getInetSocketAddress().getPort();
        this.name = host + ":" + port;
    }

    public void destroy() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HazelcastNode other = (HazelcastNode) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HazelcastNode{" + "id=" + id + ", name=" + name + ", host=" + host + ", port=" + port + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param hzMember the hzMember to set
     */
    public void setHzMember(Member hzMember) {
        this.init(hzMember);
    }
}
