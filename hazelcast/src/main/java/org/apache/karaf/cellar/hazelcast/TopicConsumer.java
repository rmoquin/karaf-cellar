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

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes messages from the Hazelcast {@code ITopic} and calls the {@code EventDispatcher}.
 */
public class TopicConsumer<E extends Event> implements EventConsumer<E>, MessageListener<E> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(TopicConsumer.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.topic.consumer";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private ITopic topic;
    private Dispatcher dispatcher;
    private NodeConfiguration nodeConfiguration;
    private CellarCluster masterCluster;
    private boolean isConsuming;
    private String listenerId;

    public void init() {
        if (topic == null) {
            topic = ((HazelcastCluster) masterCluster).getTopic(Constants.TOPIC);
        }
        start();
    }

    public void destroy() {
        stop();
    }

    /**
     * Consume a cluster event form the topic.
     *
     * @param event the cluster event.
     */
    @Override
    public void consume(E event) {
        // check if event has a specified destination.
        if ((event.getDestinations() == null || event.getDestinations().contains(masterCluster.getLocalNode())) && (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce())) {
            dispatcher.dispatch(event);
        } else if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR HAZELCAST: {} switch is OFF, cluster event is not consumed {}", SWITCH_ID);
        } else {
            LOGGER.warn("The received event will not be processed {}", event.toString());
        }
    }

    @Override
    public void start() {
        isConsuming = true;
        listenerId = topic.addMessageListener(this);
    }

    @Override
    public void stop() {
        isConsuming = false;
        if (topic != null) {
            topic.removeMessageListener(listenerId);
        }
        listenerId = null;
    }

    @Override
    public Boolean isConsuming() {
        return isConsuming;
    }

    @Override
    public void onMessage(Message<E> message) {
        consume(message.getMessageObject());
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ITopic getTopic() {
        return topic;
    }

    public void setTopic(ITopic topic) {
        this.topic = topic;
    }

    @Override
    public Switch getSwitch() {
        if (nodeConfiguration.isConsumer()) {
            eventSwitch.turnOn();
        } else {
            eventSwitch.turnOff();
        }
        return eventSwitch;
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
     * @return the nodeConfiguration
     */
    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    /**
     * @param nodeConfiguration the nodeConfiguration to set
     */
    public void setNodeConfiguration(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
    }
}
