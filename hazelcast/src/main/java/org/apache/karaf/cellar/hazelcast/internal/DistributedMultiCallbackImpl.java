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

import org.apache.karaf.cellar.core.command.DistributedMultiCallback;
import com.hazelcast.core.Member;
import com.hazelcast.core.MultiExecutionCallback;
import java.util.Map;

/**
 *
 * @author rmoquin
 */
public class DistributedMultiCallbackImpl implements MultiExecutionCallback {
    private final DistributedMultiCallback callback;

    public DistributedMultiCallbackImpl(DistributedMultiCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onResponse(Member member, Object value) {
        /*    Map<Member, Future<R>> executedResult = this.executorService.submitToMembers(task, members);
        for (Map.Entry<Member, Future<R>> entry : executedResult.entrySet()) {
            Member member = entry.getKey();
            Future<R> future = entry.getValue();
            results.put(new HazelcastNode(member), future);
        }
        LOGGER.info("Completed task {}" + task);*/
//        callback.onResponse(member, value);
    }

    @Override
    public void onComplete(Map<Member, Object> values) {
        /*Map<Member, Future<R>> executedResult = this.executorService.submitToMembers(task, members);
        for (Map.Entry<Member, Future<R>> entry : executedResult.entrySet()) {
            Member member = entry.getKey();
            Future<R> future = entry.getValue();
            results.put(new HazelcastNode(member), future);
        }*/
        //LOGGER.info("Completed task {}" + task);
//        callback.onComplete(values);
    }
}
