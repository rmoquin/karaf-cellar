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
package org.apache.karaf.cellar.hazelcast.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class InitClassesActivator  implements BundleActivator {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(InitClassesActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        LOGGER.info("Since hazelcast wants to be a pain in the butt with classloading, let's just beat it to the punch (Hopefully).");
        context.getBundle().loadClass("com.hazelcast.core.HazelcastInstance");
        context.getBundle().loadClass("com.hazelcast.core.Hazelcast");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        //Not needed for anything.. I don't think..
    }
}
