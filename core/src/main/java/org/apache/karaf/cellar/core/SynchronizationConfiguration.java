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
package org.apache.karaf.cellar.core;

import java.util.Dictionary;

/**
 *
 * @author rmoquin
 */
public interface SynchronizationConfiguration {
    /**
     * Saves any changes made to this configuration object.
     */
    public void save();

    /**
     * @param name the name of the property to set.
     */
    public Object getProperty(String name);

    /**
     * @return a single property
     */
    public void setProperty(String name, Object value);

    /**
     * @return the properties
     */
    public Dictionary<String, Object> getProperties();

    /**
     * @param properties the properties to set
     */
    public void setProperties(Dictionary<String, Object> properties);
}
