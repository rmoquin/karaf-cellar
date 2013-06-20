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
package org.apache.karaf.cellar.samples.dosgi.greeter.client;

import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greeter;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "dosgi-greeter", name = "greet", description = "Starts the greet client")
public class GreetCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "greetMessage", description = "The message that will be sent as the greeting.", required = true, multiValued = false)
    String greetMessage;

    @Argument(index = 1, name = "iterations", description = "The number of greet iterations to perform", required = false, multiValued = false)
    Integer iterations = 10;

    private Greeter greeter;

    @Override
    protected Object doExecute() throws Exception {
        GreeterClient greeterClient = new GreeterClient(greeter, greetMessage,iterations);
        greeterClient.start();
        return null;
    }

    public Greeter getGreeter() {
        return greeter;
    }

    public void setGreeter(Greeter greeter) {
        this.greeter = greeter;
    }

}
