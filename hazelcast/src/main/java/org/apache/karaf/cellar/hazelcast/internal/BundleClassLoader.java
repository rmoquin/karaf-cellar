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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.apache.karaf.cellar.hazelcast.HazelcastBundleListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rmoquin
 */
public class BundleClassLoader extends ClassLoader {
    private static Logger LOGGER = LoggerFactory.getLogger(BundleClassLoader.class);
    private BundleContext bundleContext;
    private Bundle bundle;
    private HazelcastBundleListener bundleListener;
    private int length = "META-INF/services/".length();

    public void init() {
        this.bundle = bundleContext.getBundle();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = this.bundle.loadClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        name = name.substring(length);
        if (bundleListener.getResources().containsKey(name)) {
            return bundleListener.getResources().get(name).get(0);
        } else {
            return bundle.getEntry(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        name = name.substring(length);
        LOGGER.warn(name);
        LOGGER.warn(bundleListener.getResources().toString());
        if (bundleListener.getResources().containsKey(name)) {
            return bundleListener.getResources().get(name).elements();
        } else {
            return bundle.findEntries(name, "*", false);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BundleClassLoader");
        sb.append("{_bundle=").append(bundle);
        sb.append('}');
        return sb.toString();
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @param bundleContext the bundleContext to set
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * @return the bundleListener
     */
    public HazelcastBundleListener getBundleListener() {
        return bundleListener;
    }

    /**
     * @param bundleListener the bundleListener to set
     */
    public void setBundleListener(HazelcastBundleListener bundleListener) {
        this.bundleListener = bundleListener;
    }
}
