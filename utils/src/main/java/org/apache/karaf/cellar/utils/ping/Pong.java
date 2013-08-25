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
package org.apache.karaf.cellar.utils.ping;

import org.apache.karaf.cellar.core.command.DistributedResult;

/**
 * Cluster pong event.
 */
public class Pong implements DistributedResult {
    private boolean isSuccessful = true;
    private Throwable error;

    public Pong() {
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public Throwable getThrowable() {
        return error;
    }

    /**
     * @param isSuccessful the isSuccessful to set
     */
    public void setIsSuccessful(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    /**
     * @param error the error to set
     */
    public void setThrowable(Throwable error) {
        this.error = error;
    }

}
