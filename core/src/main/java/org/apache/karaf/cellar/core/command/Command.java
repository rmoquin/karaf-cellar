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

import org.apache.karaf.cellar.core.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command.
 */
public class Command<R extends DistributedResult> extends Event {

    protected static final transient Logger LOGGER = LoggerFactory.getLogger(Command.class);

    protected long timeout = 10000;

    public Command() {
    }

    public Command(String id) {
        super(id);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
