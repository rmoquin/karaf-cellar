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
import com.hazelcast.core.ITopic;
import org.apache.karaf.cellar.core.CellarCluster;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.SynchronizationConfiguration;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;

/**
 * An event transport factory powered by Hazelcast.
 */
public class HazelcastEventTransportFactory implements EventTransportFactory {
    private Dispatcher dispatcher;
    private SynchronizationConfiguration synchronizationConfig;
    private CellarCluster masterCluster;

    @Override
    public EventProducer getEventProducer(String name, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = ((HazelcastCluster)masterCluster).getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
            TopicProducer producer = new TopicProducer();
            producer.setTopic(topic);
            producer.setMasterCluster(masterCluster);
            producer.setSynchronizationConfig(synchronizationConfig);
            producer.init();
            return producer;
        } else {
            IQueue queue = ((HazelcastCluster)masterCluster).getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
            QueueProducer producer = new QueueProducer();
            producer.setQueue(queue);
            producer.setMasterCluster(masterCluster);
            producer.setSynchronizationConfig(synchronizationConfig);
            producer.init();
            return producer;
        }
    }

    @Override
    public EventConsumer getEventConsumer(String name, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = ((HazelcastCluster)masterCluster).getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
            TopicConsumer consumer = new TopicConsumer();
            consumer.setTopic(topic);
            consumer.setSynchronizationConfig(synchronizationConfig);
            consumer.setDispatcher(dispatcher);
            consumer.setMasterCluster(masterCluster);
            consumer.init();
            return consumer;
        } else {
            IQueue queue = ((HazelcastCluster)masterCluster).getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
            QueueConsumer consumer = new QueueConsumer();
            consumer.setQueue(queue);
            consumer.setDispatcher(dispatcher);
            consumer.setSynchronizationConfig(synchronizationConfig);
            consumer.init();
            return consumer;
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
}
