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

import java.net.URL;
import org.osgi.framework.Bundle;

/**
 *
 * @author rmoquin
 */
public class HazelcastClassLoader extends ClassLoader {
    private transient Bundle bundle;

    public HazelcastClassLoader() {
    }

    public HazelcastClassLoader(Bundle bundle) {
        super();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (bundle == null) {
            return super.findClass(name);
        } else {
            return bundle.loadClass(name);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (bundle == null) {
            return super.loadClass(name);
        } else {
            return bundle.loadClass(name);
        }
    }

    @Override
    public URL getResource(String name) {
        if (bundle == null) {
            return super.getResource(name);
        } else {
            return bundle.getEntry(name);
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
}
