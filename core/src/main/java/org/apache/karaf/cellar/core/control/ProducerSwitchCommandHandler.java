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
package org.apache.karaf.cellar.core.control;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;

/**
 * Producer switch command handler.
 */
public class ProducerSwitchCommandHandler extends CommandHandler<ProducerSwitchCommand, ProducerSwitchResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.producer.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    /**
     * Execute a producer switch command.
     *
     * @param command the producer switch command to execute.
     * @return the result of the command execution.
     */
    @Override
    public ProducerSwitchResult execute(ProducerSwitchCommand command) {
        // query
        if (command.getStatus() == null) {
            return new ProducerSwitchResult(command.getId(), Boolean.TRUE, producer.getSwitch().getStatus().getValue());
        } else if (command.getStatus().equals(SwitchStatus.ON)) {
            // turn on the switch
            producer.getSwitch().turnOn();
            // persist the change
            persist(command.getStatus());
            return new ProducerSwitchResult(command.getId(), Boolean.TRUE, Boolean.TRUE);
        } else if (command.getStatus().equals(SwitchStatus.OFF)) {
            // turn off the switch
            producer.getSwitch().turnOff();
            // persist the change
            persist(command.getStatus());
            return new ProducerSwitchResult(command.getId(), Boolean.TRUE, Boolean.FALSE);
        } else {
            return new ProducerSwitchResult(command.getId(), Boolean.FALSE, producer.getSwitch().getStatus().getValue());
        }
    }

    /**
     * Store the producer current status in ConfigurationAdmin.
     *
     * @param switchStatus the producer switch status to store.
     */
    private void persist(SwitchStatus switchStatus) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties != null) {
                    properties.put(Configurations.PRODUCER, switchStatus.getValue().toString());
                    configuration.update(properties);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Can't persist the producer status", e);
        }
    }

    @Override
    public Class<ProducerSwitchCommand> getType() {
        return ProducerSwitchCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

    @Override
    public Producer getProducer() {
        return producer;
    }

    @Override
    public void setProducer(Producer producer) {
        this.producer = producer;
    }

}
