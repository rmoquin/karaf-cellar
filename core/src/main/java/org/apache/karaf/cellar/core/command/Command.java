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
package org.apache.karaf.cellar.core.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.karaf.cellar.core.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.Node;

/**
 * Command.
 */
@JsonTypeName("managedGroupCommand")
public class Command<R extends Result> extends Event {
    protected static final transient Logger LOGGER = LoggerFactory.getLogger(Command.class);
    protected long timeout = 10000;
    @JsonDeserialize(keyAs=Node.class, contentAs = Result.class)
    protected final BlockingQueue resultQueue = new LinkedBlockingQueue();
    @JsonDeserialize(keyAs=Node.class, contentAs = Result.class)
    protected final Map<Node, R> nodeResults = new HashMap<Node, R>();

    public Command() {
        this.force = true;
    }

    public Command(String id) {
        super(id);
        this.force = true;
    }

    @Override
    public Boolean getForce() {
        return true;
    }

    /**
     * Process the event of timeout.
     */
    public void onTimeout() {
        try {
            resultQueue.put(nodeResults);
        } catch (InterruptedException e) {
            LOGGER.error("Error adding result to result queue", e);
        }
    }

    /**
     * Add {@code Results} to the result queue.
     *
     * @param results the results in the queue.
     */
    public void addResults(R... results) {
        if (results != null && results.length > 0) {
            for (R result : results) {
                nodeResults.put(result.getSourceNode(), result);
            }

            if (getDestinations() == null || (nodeResults.size() == getDestinations().size())) {
                try {
                    resultQueue.put(nodeResults);
                } catch (InterruptedException e) {
                    LOGGER.error("Error adding result to result queue", e);
                }
            }
        }
    }

    /**
     * Return the responses.
     * If no result is found, it returns an empty map.
     *
     * @return a map of results.
     * @throws Exception in case of interruption.
     */
    public Map<Node, R> getResult() throws InterruptedException {
        Map<Node, R> results = null;
        if (this.resultQueue != null) {
            results = resultQueue.poll(timeout, TimeUnit.MILLISECONDS);
        }
        return results;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
