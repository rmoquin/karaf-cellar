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

import org.apache.karaf.cellar.core.command.DistributedCallback;
import com.hazelcast.core.ExecutionCallback;

/**
 *
 * @author rmoquin
 */
public class DistributedCallbackImpl<T> implements ExecutionCallback<T>{
    private final DistributedCallback<T> callback;

    public DistributedCallbackImpl(DistributedCallback<T> callback) {
        this.callback = callback;
    }
    
    @Override
    public void onResponse(T response) {
        callback.onResponse(response);
    }

    @Override
    public void onFailure(Throwable t) {
        callback.onFailure(t);
    }
    
}
