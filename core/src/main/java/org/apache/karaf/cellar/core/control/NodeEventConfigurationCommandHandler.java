/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.karaf.cellar.core.control;

import java.io.IOException;
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class NodeEventConfigurationCommandHandler extends CommandHandler<NodeConfigurationCommand, NodeConfigurationResult> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(NodeEventConfigurationCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.nodeconfig.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    public NodeEventConfigurationCommandHandler() {
    }

    @Override
    public NodeConfigurationResult execute(NodeConfigurationCommand command) {
        NodeConfigurationResult result = new NodeConfigurationResult();
        try {
            SwitchType type = command.getType();
            SwitchStatus status = command.getStatus();
            if (SwitchType.INBOUND.equals(type)) {
                nodeConfiguration.setConsumer(SwitchStatus.ON.equals(status));
            } else if (SwitchType.OUTBOUND.equals(type)) {
                nodeConfiguration.setProducer(SwitchStatus.ON.equals(status));
            }
            Configuration configuration = getConfigAdmin().getConfiguration(NodeConfiguration.class.getCanonicalName());
            configuration.update(nodeConfiguration.getProperties());
            result.setSwitchStatus(status);
            result.setSwitchType(type);
            result.setSuccessful(true);
        } catch (IOException ex) {
            LOGGER.error("Task wasn't processed for some reason.", ex);
            result.setThrowable(ex);
            result.setSuccessful(false);
        }
        return result;
    }

    @Override
    public Class<NodeConfigurationCommand> getType() {
        return NodeConfigurationCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }
}
