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
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;

/**
 * An event transport factory powered by Hazelcast.
 */
public class HazelcastEventTransportFactory implements EventTransportFactory {

    private Dispatcher dispatcher;
    
    @Override
    public EventProducer getEventProducer(CellarCluster cluster, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = ((HazelcastCluster)cluster).getTopic(Constants.TOPIC + Constants.SEPARATOR + cluster.getName());
            TopicProducer producer = new TopicProducer();
            producer.setTopic(topic);
            producer.init(cluster);
            return producer;
        } else {
            IQueue queue = ((HazelcastCluster)cluster).getQueue(Constants.QUEUE + Constants.SEPARATOR + cluster.getName());
            QueueProducer producer = new QueueProducer();
            producer.setQueue(queue);
            producer.init(cluster);
            return producer;
        }
    }

    @Override
    public EventConsumer getEventConsumer(CellarCluster cluster, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = ((HazelcastCluster)cluster).getTopic(Constants.TOPIC + Constants.SEPARATOR + cluster.getName());
            TopicConsumer consumer = new TopicConsumer();
            consumer.setTopic(topic);       
            consumer.setDispatcher(dispatcher);
            consumer.init(cluster);
            return consumer;
        } else {
            IQueue queue = ((HazelcastCluster)cluster).getQueue(Constants.QUEUE + Constants.SEPARATOR + cluster.getName());
            QueueConsumer consumer = new QueueConsumer();
            consumer.setQueue(queue);
            consumer.setDispatcher(dispatcher);
            consumer.init(cluster);
            return consumer;
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
