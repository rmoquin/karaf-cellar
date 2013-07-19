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

import java.util.Dictionary;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer switch command handler.
 */
public class ProducerSwitchCommandHandler extends CommandHandler<ProducerSwitchCommand, ProducerSwitchResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ProducerSwitchCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.producer.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private ConfigurationAdmin configurationAdmin;

    /**
     * Execute a producer switch command.
     *
     * @param command the producer switch command to execute.
     * @return the result of the command execution.
     */
    @Override
    public ProducerSwitchResult execute(ProducerSwitchCommand command) {
        SwitchStatus status = command.getStatus();
        Boolean currentStatus = producer.getSwitch().getStatus().getValue();
        if (status == null) {
            return new ProducerSwitchResult(command.getId(), Boolean.TRUE, currentStatus);
        } else {
            Boolean statusValue = status.getValue();
            if (statusValue) {
                producer.getSwitch().turnOn();
            } else {
                producer.getSwitch().turnOff();
            }
            try {
                Configuration configuration = this.configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
                Dictionary properties = configuration.getProperties();
                properties.put(Configurations.PRODUCER, statusValue);
                configuration.update(properties);
            } catch (Exception ex) {
                LOGGER.warn("Error setting producer switch.", ex);
                return new ProducerSwitchResult(command.getId(), Boolean.FALSE, currentStatus);
            }
            return new ProducerSwitchResult(command.getId(), Boolean.TRUE, statusValue);
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

    /**
     * @return the configurationAdmin
     */
    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    /**
     * @param configurationAdmin the configurationAdmin to set
     */
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
