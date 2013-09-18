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

/**
 *
 * @author rmoquin
 */
public interface DistributedResult extends Serializable {
    
    /**
     * Whether or not the task was successful.
     *
     * @return true if successful.
     */
    public boolean isSuccessful();

    /**
     * If the result wasn't successful then there should be an exception stored.
     *
     * @return an exception if one occurred, otherwise null.
     */
    public Throwable getThrowable();
}
