/*
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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.IQueue;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.SwitchConfiguration;
import org.apache.karaf.cellar.core.command.Result;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produces cluster {@code Event}s into the Hazelcast {@code IQueue}.
 */
public class QueueProducer<E extends Event> implements EventProducer<E> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueProducer.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.producer";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private SwitchConfiguration switchConfig;
    private IQueue<E> queue;
    private CellarCluster masterCluster;

    public void init() {
    }

    public void destroy() {
        // nothing to do
    }

    @Override
    public void produce(E event) {
        if (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce() || event instanceof Result) {
            event.setSourceNode(this.masterCluster.getLocalNode());
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                LOGGER.error("CELLAR HAZELCAST: queue producer interrupted", e);
            }
        } else {
            if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
                LOGGER.warn("CELLAR HAZELCAST: {} switch is OFF, don't produce the cluster event", SWITCH_ID);
            }
        }
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Boolean status = Boolean.parseBoolean((String) switchConfig.getProperty(Configurations.PRODUCER));
            if (status) {
                eventSwitch.turnOn();
            } else {
                eventSwitch.turnOff();
            }
        } catch (Exception e) {
            // ignore
        }
        return eventSwitch;
    }

    public IQueue<E> getQueue() {
        return queue;
    }

    public void setQueue(IQueue<E> queue) {
        this.queue = queue;
    }

    /**
     * @return the masterCluster
     */
    public CellarCluster getMasterCluster() {
        return masterCluster;
    }

    /**
     * @param masterCluster the masterCluster to set
     */
    public void setMasterCluster(CellarCluster masterCluster) {
        this.masterCluster = masterCluster;
    }

    /**
     * @return the switchConfig
     */
    public SwitchConfiguration getSwitchConfig() {
        return switchConfig;
    }

    /**
     * @param switchConfig the switchConfig to set
     */
    public void setSwitchConfig(SwitchConfiguration switchConfig) {
        this.switchConfig = switchConfig;
    }
}
