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
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.SynchronizationConfiguration;

/**
 * Consumes cluster events from the Hazelcast {@code IQueue} and calls the {@code EventDispatcher}.
 */
public class QueueConsumer<E extends Event> implements EventConsumer<E>, ItemListener<E>, Runnable {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueConsumer.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.consumer";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Boolean isConsuming = Boolean.TRUE;
    private SynchronizationConfiguration synchronizationConfig;
    private IQueue queue;
    private Dispatcher dispatcher;
//    private String listenerId;

    public void init() {
//        listenerId = queue.addItemListener(this, true);
        queue.addItemListener(this, true);
        executorService.execute(this);
    }

    public void destroy() {
        isConsuming = false;
        if (queue != null) {
//            queue.removeItemListener(listenerId);
            queue.removeItemListener(this);
        }
        executorService.shutdown();
    }

    @Override
    public void run() {
        try {
            while (isConsuming) {
                E e = null;
                try {
                    e = getQueue().poll(5, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("CELLAR HAZELCAST: consume task interrupted");
                }
                if (e != null) {
                    consume(e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR HAZELCAST: failed to consume from queue", ex);
        }
    }

    /**
     * Consume a cluster event.
     *
     * @param event the cluster event.
     */
    @Override
    public void consume(E event) {
        if (event != null && (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce())) {
            dispatcher.dispatch(event);
        } else {
            if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
                LOGGER.warn("CELLAR HAZELCAST: {} switch is OFF, cluster event is not consumed", SWITCH_ID);
            }
        }
    }

    @Override
    public void start() {
        isConsuming = true;
        executorService.execute(this);
    }

    @Override
    public void stop() {
        isConsuming = false;
    }

    @Override
    public Boolean isConsuming() {
        return isConsuming;
    }

    @Override
    public void itemAdded(ItemEvent<E> event) {
        LOGGER.info("An item was added: " + event.toString());
    }

    @Override
    public void itemRemoved(ItemEvent<E> event) {
        LOGGER.info("An item was removed: " + event.toString());
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public IQueue<E> getQueue() {
        return queue;
    }

    public void setQueue(IQueue<E> queue) {
        this.queue = queue;
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Boolean status = Boolean.parseBoolean((String) synchronizationConfig.getProperty(Configurations.CONSUMER));
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

    /**
     * @return the synchronizationConfig
     */
    public SynchronizationConfiguration getSynchronizationConfig() {
        return synchronizationConfig;
    }

    /**
     * @param synchronizationConfig the synchronizationConfig to set
     */
    public void setSynchronizationConfig(SynchronizationConfiguration synchronizationConfig) {
        this.synchronizationConfig = synchronizationConfig;
    }
}
