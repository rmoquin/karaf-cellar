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
import java.text.MessageFormat;
import org.apache.karaf.cellar.core.Node;

/**
 * Cluster node powered by Hazelcast.
 */
public class HazelcastNode implements Node {
    private String host;
    private int port;
    private Member hzMember;

    public HazelcastNode() {
    }
    
    public HazelcastNode(Member hzMember) {
        this.hzMember = hzMember;
    }

    public void init(Member hzMember) {
        this.host = this.hzMember.getInetSocketAddress().getHostString();
        this.port = this.hzMember.getInetSocketAddress().getPort();
    }

    public void destroy() {
        this.hzMember = null;
    }

    @Override
    public String getId() {
        return hzMember.getUuid();
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HazelcastNode that = (HazelcastNode) o;
        String id = this.hzMember.getUuid();
        if (id != null ? !id.equals(that.getId()) : that.getId() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        String id = this.hzMember.getUuid();
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return MessageFormat.format("HazelcastNode [id={0}, host={1}, port={2}]", getId(), host, port);
    }
}
