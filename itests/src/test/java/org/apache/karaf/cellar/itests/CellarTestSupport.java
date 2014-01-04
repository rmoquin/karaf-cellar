/*
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
package org.apache.karaf.cellar.itests;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class CellarTestSupport {

    public static final Long DELAY_TIMEOUT = 5000L;
    public static final Long COMMAND_TIMEOUT = 10000L;
    public static final Long SERVICE_TIMEOUT = 30000L;
    public static final String SSH_PORT = "8101";
    static final String KARAF_VERSION = "3.0.0";
    static final String CELLAR_VERSION = "3.0.0-SNAPSHOT";
    static final String INSTANCE_STARTED = "Started";
    static final String INSTANCE_STARTING = "Starting";
    static final String DEBUG_OPTS = " --java-opts \"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s\"";
    ExecutorService executor = Executors.newCachedThreadPool();
    @Inject
    protected BundleContext bundleContext;
    @Inject
    protected FeaturesService featureService;
    @Inject
    protected InstanceService instanceService;
    @Inject
    BootFinished bootFinished;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    public File getConfigFile(String path) {
        return new File(this.getClass().getResource(path).getFile());
    }

    /**
     * Installs the Cellar feature
     */
    protected void installCellar() {
        try {
            featureService.addRepository(new URI(getCellarUri()), false);
            featureService.installFeature("cellar");
        } catch (Exception ex) {
            throw new RuntimeException("Error installing cellar feature", ex);
        }
    }

    protected void unInstallCellar() {
        try {
            featureService.uninstallFeature("cellar");
        } catch (Exception ex) {
            throw new RuntimeException("Error uninstalling cellar feature", ex);
        }
    }

    /**
     * Creates one or more karaf containers with cellar installed.
     *
     * @param names of the new cellar child instances.
     */
    protected void createCellarChild(String... names) throws Exception {
        final ClusterManager manager = this.getOsgiService(ClusterManager.class, SERVICE_TIMEOUT);
        //Create and start each child node
        List<String> startingNodes = new ArrayList<String>(names.length);
        List<String> startedNodes = new ArrayList<String>(names.length);
        List<String> connectingNodes = new ArrayList<String>(names.length);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            System.err.println(executeCommand("instance:create --featureURL " + getCellarUri() + " " + name));
            System.err.println(executeCommand("instance:start " + name));
            startingNodes.add(name);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //Ignore
        }

        //Wait till the node is listed as Started.
        for (int i = 0; i < 30; i++) {
            //Wait till the node is listed as Started.
            for (Iterator<String> it = startingNodes.iterator(); it.hasNext();) {
                String name = it.next();
                Instance instance = instanceService.getInstance(name);
                System.err.println("Checking state for instance with name: " + name + ", " + instance.getState());
                if (Instance.STARTED.equals(instance.getState())) {
                    System.err.println(executeRemoteCommand(name, "feature:install cellar"));
                    it.remove();
                    startedNodes.add(name);
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                }
            }
            for (Iterator<String> it = startedNodes.iterator(); it.hasNext();) {
                String name = it.next();
                System.err.println("Checking to see if instance, " + name + ", connected to cluster.");
                String nodeId = this.getNodeIdOfChild(name);
                if (nodeId != null) {
                    it.remove();
                    connectingNodes.add(nodeId);
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                }
            }
            for (Iterator<String> it = connectingNodes.iterator(); it.hasNext();) {
                String name = it.next();
                if (manager.findNodeById(name) != null) {
                    it.remove();
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                }
            }
            if (startingNodes.isEmpty() && connectingNodes.isEmpty() && startedNodes.isEmpty()) {
                System.err.println("All node(s) " + Arrays.toString(names) + " are registered.");
                return;
            } else {
                System.out.print(".");
            }
        }
        if (!startingNodes.isEmpty() && !connectingNodes.isEmpty() && !startedNodes.isEmpty()) {
            throw new RuntimeException("Failed waiting for node(s) in starting state " + startingNodes + " and node(s) in started state " + startedNodes
                    + " and node(s) in connecting state " + connectingNodes);
        }
    }

    protected String executeRemoteCommand(String name, String command) {
        String instanceCmd = "instance:connect -u karaf -p karaf " + name + " ";
        return executeCommand(instanceCmd + command);
    }

    /**
     * Destroys the child node.
     */
    protected void destroyCellarChild(String name) {
        try {
            System.err.println(executeRemoteCommand(name, "feature:uninstall cellar"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        try {
            System.err.println(executeCommand("instance:stop " + name));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Returns the node id of a specific child instance.
     *
     * @param name
     * @return
     */
    protected String getNodeIdOfChild(String name) {
        String nodesList = executeRemoteCommand(name, "cluster:node-list | grep \\\\*");
        int stop = nodesList.indexOf(']');
        if (stop > -1) {
            String node = nodesList.substring(0, stop);
            int start = node.lastIndexOf('[');
            if (start > -1) {
                node = node.substring(start + 1);
                node = node.trim();
                return node;
            }
        }
        return null;
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").version(KARAF_VERSION).type("tar.gz");
        Option[] options = new Option[]{
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
            // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
            configureSecurity().enableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
            //            editConfigurationFileExtend("etc/system.properties", "cellar.feature.url", getCellarUri()),
            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "config,standard,region,package,kar,ssh,management")};
        //            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),};
        String debug = System.getProperty("debugMain");
        if (debug != null) {
            int l = options.length;
            options = Arrays.copyOf(options, l + 1);
            options[l] = KarafDistributionOption.debugConfiguration();
        }
        return options;
    }

    protected String getCellarUri() {
        return maven().groupId("org.apache.karaf.cellar").artifactId("apache-karaf-cellar").version(CELLAR_VERSION).classifier("features").type("xml").getURL();
    }

    public String generateSSH(String instanceName, String command) {
        return "ssh:ssh -q -l karaf -P karaf -p " + SSH_PORT + " 127.0.0.1 " + command;
    }

    /**
     * Executes a shell command and returns output as a String. Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    protected String executeCommand(final String command, Principal... principals) {
        return executeCommand(command, COMMAND_TIMEOUT, false, principals);
    }

    /**
     * Executes a shell command and returns output as a String. Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent Specifies if the command should be displayed in the screen.
     * @param principals The principals (e.g. RolePrincipal objects) to run the command under
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal... principals) {
        waitForCommandService(command);

        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);

        final Callable<String> commandCallable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    if (!silent) {
                        System.err.println(command);
                    }
                    commandSession.execute(command);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                } finally {
                    printStream.flush();
                }
                return byteArrayOutputStream.toString();
            }
        };

        FutureTask<String> commandFuture;
        if (principals.length == 0) {
            commandFuture = new FutureTask<String>(commandCallable);
        } else {
            // If principals are defined, run the command callable via Subject.doAs()
            commandFuture = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Subject subject = new Subject();
                    subject.getPrincipals().addAll(Arrays.asList(principals));
                    return Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
                        @Override
                        public String run() throws Exception {
                            return commandCallable.call();
                        }
                    });
                }
            });
        }

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }
        return response;
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            System.err.println("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
//                Dictionary dic = bundleContext.getBundle().getHeaders();
//                System.err.println("Test bundle headers: " + explode(dic));
//
//                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
//                    System.err.println("ServiceReference: " + ref);
//                }
//
//                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
//                    System.err.println("Filtered ServiceReference: " + ref);
//                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForCommandService(String command) {
        // the commands are represented by services. Due to the asynchronous nature of services they may not be
        // immediately available. This code waits the services to be available, in their secured form. It
        // means that the code waits for the command service to appear with the roles defined.

        if (command == null || command.length() == 0) {
            return;
        }

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0) {
            command = command.substring(0, spaceIdx);
        }
        int colonIndx = command.indexOf(':');

        try {
            if (colonIndx > 0) {
                String scope = command.substring(0, colonIndx);
                String function = command.substring(colonIndx + 1);
                waitForService("(&(osgi.command.scope=" + scope + ")(osgi.command.function=" + function + "))", SERVICE_TIMEOUT);
            } else {
                waitForService("(osgi.command.function=" + command + ")", SERVICE_TIMEOUT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForService(String filter, long timeout) throws InvalidSyntaxException, InterruptedException {
        ServiceTracker<Object, Object> st = new ServiceTracker<Object, Object>(bundleContext, bundleContext.createFilter(filter), null);
        try {
            st.open();
            st.waitForService(timeout);
        } finally {
            st.close();
        }
    }
}
