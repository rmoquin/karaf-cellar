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
import org.apache.karaf.cellar.core.NodeConfiguration;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Manage handlers command handler.
 */
public class ManageHandlersCommandHandler extends CommandHandler<ManageHandlersCommand, ManageHandlersResult> {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ManageHandlersCommandHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.listhandlers.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);
    private ConfigurationAdmin configurationAdmin;

    /**
     * Return a map containing all managed {@code EventHandler}s and their status.
     *
     * @param command the manage handlers command to execute.
     * @return a result containing the map of event handlers (and their status).
     */
    @Override
    public ManageHandlersResult execute(ManageHandlersCommand command) {
        ManageHandlersResult result = new ManageHandlersResult(command.getId());

        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference[] references = null;
        try {
            references = bundleContext.getServiceReferences(EventHandler.class.getName(), EventHandler.MANAGED_FILTER);
            if (references != null && references.length > 0) {
                for (ServiceReference ref : references) {
                    EventHandler handler = (EventHandler) bundleContext.getService(ref);
                    String handlerClassName = handler.getClass().getName();
                    if (command.getHandlerName() == null) {
                        result.getHandlers().put(handlerClassName, handler.getSwitch().getStatus().name());
                    } else {
                        if (command.getHandlerName().equals(handlerClassName)) {
                            if (command.getStatus() != null) {
                                if (command.getStatus()) {
                                    // persist the handler switch status to configuration admin
                                    persist(handlerClassName, SwitchStatus.ON);
                                    handler.getSwitch().turnOn();
                                } else {
                                    // persist the handler switch status to configuration admin
                                    persist(handlerClassName, SwitchStatus.OFF);
                                    handler.getSwitch().turnOff();
                                }
                            }
                            result.getHandlers().put(handlerClassName, handler.getSwitch().getStatus().name());
                            break;
                        }
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Syntax error looking up service {} using filter {}", EventHandler.class.getName(), EventHandler.MANAGED_FILTER, e);
        } finally {
            if (references != null) {
                for (ServiceReference ref : references) {
                    bundleContext.ungetService(ref);
                }
            }
        }
        return result;
    }

    /**
     * Store the handler switch configuration in configuration admin.
     *
     * @param handler the handler to store.
     * @param switchStatus the switch status to store.
     */
    private void persist(String handler, SwitchStatus switchStatus) {
        try {
            Configuration configuration = this.configurationAdmin.getConfiguration(NodeConfiguration.class.getCanonicalName(), "?");
            Dictionary properties = configuration.getProperties();
            properties.put(handler, switchStatus.getValue());
        } catch (Exception e) {
            LOGGER.warn("Can't persist the handler " + handler + " status", e);
        }
    }

    @Override
    public Class<ManageHandlersCommand> getType() {
        return ManageHandlersCommand.class;
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
