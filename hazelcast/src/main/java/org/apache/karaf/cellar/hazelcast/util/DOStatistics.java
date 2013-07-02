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
package org.apache.karaf.cellar.hazelcast.util;

import com.hazelcast.core.EntryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class DOStatistics {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(DOStatistics.class);
    //In case this method is helpful, found it in the docs and thought maybe I'd just save it somewhere.
    public void printStatistics(EntryView entry) {
        //EntryView entry = hz.getMap("quotes").getEntryView("1");
        LOGGER.debug("size in memory  : " + entry.getCost());
        LOGGER.debug("creationTime    : " + entry.getCreationTime());
        LOGGER.debug("expirationTime  : " + entry.getExpirationTime());
        LOGGER.debug("number of hits  : " + entry.getHits());
        LOGGER.debug("lastAccessedTime: " + entry.getLastAccessTime());
        LOGGER.debug("lastUpdateTime  : " + entry.getLastUpdateTime());
        LOGGER.debug("version         : " + entry.getVersion());
        LOGGER.debug("key             : " + entry.getKey());
        LOGGER.debug("value           : " + entry.getValue());
    }
}
