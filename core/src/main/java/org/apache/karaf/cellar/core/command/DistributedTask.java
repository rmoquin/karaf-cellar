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
package org.apache.karaf.cellar.core.command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.shell.console.BundleContextAware;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author rmoquin
 */
public abstract class DistributedTask<T extends DistributedResult> implements Callable<T>, Serializable, BundleContextAware {

    protected transient BundleContext bundleContext;
    protected List<ServiceReference<?>> usedReferences;
    protected Node sourceNode;
    protected Group sourceGroup;
    protected boolean force;
    protected boolean postPublish;

    @Override
    public T call() throws Exception {
        try {
            return execute();
        } finally {
            ungetServices();
        }
    }

    abstract protected T execute() throws Exception;

    protected <T> T getService(String clazz) {
        ServiceReference<T> sr = (ServiceReference<T>) getBundleContext().getServiceReference(clazz);
        if (sr != null) {
            return getService(sr);
        } else {
            return null;
        }
    }

    protected <T> T getService(Class<T> clazz) {
        ServiceReference<T> sr = getBundleContext().getServiceReference(clazz);
        if (sr != null) {
            return getService(sr);
        } else {
            return null;
        }
    }

    protected <T> T getService(ServiceReference<T> reference) {
        T t = getBundleContext().getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference<?>>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference<?> ref : usedReferences) {
                getBundleContext().ungetService(ref);
            }
        }
    }

    /**
     * @return the sourceNode
     */
    public Node getSourceNode() {
        return sourceNode;
    }

    /**
     * @param sourceNode the sourceNode to set
     */
    public void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }

    /**
     * @return the sourceGroup
     */
    public Group getSourceGroup() {
        return sourceGroup;
    }

    /**
     * @param sourceGroup the sourceGroup to set
     */
    public void setSourceGroup(Group sourceGroup) {
        this.sourceGroup = sourceGroup;
    }

    public BundleContext getBundleContext() {
        Bundle framework = bundleContext.getBundle(0);
        return framework == null ? bundleContext : framework.getBundleContext();
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isPostPublish() {
        return postPublish;
    }

    public void setPostPublish(boolean postPublish) {
        this.postPublish = postPublish;
    }
}
