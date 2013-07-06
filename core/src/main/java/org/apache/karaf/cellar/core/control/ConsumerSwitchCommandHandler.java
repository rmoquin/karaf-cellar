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
import org.apache.karaf.cellar.core.Consumer;
import org.apache.karaf.cellar.core.SwitchConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer switch command handler.
 */
public class ConsumerSwitchCommandHandler extends CommandHandler<ConsumerSwitchCommand, ConsumerSwitchResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConsumerSwitchCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.producer.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private Consumer consumer;
    private SwitchConfiguration switchConfig;

    /**
     * Handle the {@code ConsumeSwitchCommand} command.
     *
     * @param command
     */
    @Override
    public ConsumerSwitchResult execute(ConsumerSwitchCommand command) {
        SwitchStatus status = command.getStatus();
        Boolean currentStatus = consumer.getSwitch().getStatus().getValue();
        if (status == null) {
            return new ConsumerSwitchResult(command.getId(), Boolean.TRUE, currentStatus);
        } else {
            Boolean statusValue = status.getValue();
            if (statusValue) {
                consumer.getSwitch().turnOn();
            } else {
                consumer.getSwitch().turnOff();
            }
            try {
                if (this.switchConfig != null) {
                    this.switchConfig.setProperty(Configurations.CONSUMER, statusValue);
                    this.switchConfig.save();
                }
            } catch (Exception ex) {
                LOGGER.warn("Error setting consumer switch.", ex);
                return new ConsumerSwitchResult(command.getId(), Boolean.FALSE, currentStatus);
            }
            return new ConsumerSwitchResult(command.getId(), Boolean.TRUE, statusValue);
        }
    }

    @Override
    public Class<ConsumerSwitchCommand> getType() {
        return ConsumerSwitchCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
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
