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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellarTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CellarTestSupport.class);
    public static final Long DELAY_TIMEOUT = 5000L;
    public static final Long COMMAND_TIMEOUT = 10000L;
    public static final Long SERVICE_TIMEOUT = 30000L;
    public static final String RMI_SERVER_PORT = "44445";
    public static final String HTTP_PORT = "9081";
    public static final String RMI_REG_PORT = "1100";
    static final String INSTANCE_STARTED = "Started";
    static final String INSTANCE_STARTING = "Starting";
    static final String DEBUG_OPTS = " --java-opts \"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s\"";
    protected String cellarFeatureURI;
    @Rule
    public CellarTestWatcher baseTestWatcher = new CellarTestWatcher();
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
            cellarFeatureURI = maven().groupId("org.apache.karaf.cellar").artifactId("apache-karaf-cellar").version("3.0.0-SNAPSHOT").classifier("features").type("xml").getURL();
            featureService.addRepository(new URI(cellarFeatureURI), false);
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
     * Creates a child instance that runs cellar.
     *
     * @param name of the new cellar child.
     */
    protected void createCellarChild(String name) {
        createCellarChild(name, false, 0);
    }

    protected void createCellarChild(String name, boolean debug, int port) {
        int instances = 0;
        String createCommand = "instance:create --featureURL " + cellarFeatureURI + " --feature cellar ";
        String debugOpts = "";
        if (debug && port > 0) {
            debugOpts = String.format(DEBUG_OPTS, port);
        }
        System.err.println(executeCommand(createCommand + " " + name + " " + debugOpts));
        System.err.println(executeCommand("instance:start " + name));

        //Wait till the node is listed as Starting
        System.err.print("Waiting for " + name + " to start ");
        for (int i = 0; i < 5 && instances == 0; i++) {
            String response = executeCommand("instance:list | grep " + name + " | grep -c " + INSTANCE_STARTED, COMMAND_TIMEOUT, false);
            instances = Integer.parseInt(response.trim());
            System.err.print(".");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        if (instances > 0) {
            System.err.println(".Started!");
        } else {
            System.err.println(".Timed Out!");
        }

    }

    /**
     * Destroys the child node.
     */
    protected void destroyCellarChild(String name) {
        try {
            System.err.println(executeCommand(generateSSH(name, "feature:uninstall cellar")));
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
        String nodesList = executeCommand(generateSSH(name, " cluster:node-list | grep \\\\*"), COMMAND_TIMEOUT, false);
        int stop = nodesList.indexOf(']');
        String node = nodesList.substring(0, stop);
        int start = node.lastIndexOf('[');
        node = node.substring(start + 1);
        node = node.trim();
        return node;
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");
        Option[] options = new Option[]{
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
            // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
            configureSecurity().enableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
            replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "config,standard,region,package,ssh,kar,management"),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", RMI_REG_PORT),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", RMI_SERVER_PORT)
        };
        String debug = System.getProperty("debugMain");
        if (debug != null) {
            int l = options.length;
            options = Arrays.copyOf(options, l + 1);
            options[l] = KarafDistributionOption.debugConfiguration();
        }
        return options;
    }

    protected boolean waitForInstanceToCluster(final int desiredTotal) {
        return waitForInstanceToCluster(desiredTotal, 10);
    }

    protected boolean waitForInstanceToCluster(final int desiredTotal, final int attempts) {
        final ClusterManager manager = this.getOsgiService(ClusterManager.class, SERVICE_TIMEOUT);
        try {
            for (int i = 0; i < attempts; i++) {
                Set<Node> nodes = manager.listNodes();
                if (nodes.size() >= desiredTotal) {
                    System.err.println("Total nodes found successfully: " + desiredTotal);
                    return true;
                }
                Thread.sleep(1000);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public String generateSSH(String instanceName, String command) {
        int port = instanceService.getInstance(instanceName).getSshPort();
        return "ssh:ssh -q -l karaf -P karaf -p " + port + " 127.0.0.1 " + command;
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
                }
                printStream.flush();
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
        } catch (TimeoutException e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT for command " + command;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause().getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
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

    /**
     * Provides an iterable collection of references, even if the original array is null
     */
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
    }

    public JMXConnector getJMXConnector() throws Exception {
        return getJMXConnector("karaf", "karaf");
    }

    public JMXConnector getJMXConnector(String userName, String passWord) throws Exception {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + RMI_REG_PORT + "/karaf-root");
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        String[] credentials = new String[]{userName, passWord};
        env.put("jmx.remote.credentials", credentials);
        JMXConnector connector = JMXConnectorFactory.connect(url, env);
        return connector;
    }

    public void assertFeatureInstalled(String featureName) {
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature feature : features) {
            if (featureName.equals(feature.getName())) {
                return;
            }
        }
        Assert.fail("Feature " + featureName + " should be installed but is not");
    }

    public void assertFeaturesInstalled(String... expectedFeatures) {
        Set<String> expectedFeaturesSet = new HashSet<String>(Arrays.asList(expectedFeatures));
        Feature[] features = featureService.listInstalledFeatures();
        Set<String> installedFeatures = new HashSet<String>();
        for (Feature feature : features) {
            installedFeatures.add(feature.getName());
        }
        String msg = "Expecting the following features to be installed : " + expectedFeaturesSet + " but found " + installedFeatures;
        Assert.assertTrue(msg, installedFeatures.containsAll(expectedFeaturesSet));
    }

    public void assertContains(String expectedPart, String actual) {
        assertTrue("Should contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

    public void assertContainsNot(String expectedPart, String actual) {
        Assert.assertFalse("Should not contain '" + expectedPart + "' but was : " + actual, actual.contains(expectedPart));
    }

    protected void assertBundleInstalled(String name) {
        Assert.assertNotNull("Bundle " + name + " should be installed", findBundleByName(name));
    }

    protected void assertBundleNotInstalled(String name) {
        Assert.assertNull("Bundle " + name + " should not be installed", findBundleByName(name));
    }

    protected Bundle findBundleByName(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    protected void installAndAssertFeature(String feature) throws Exception {
        featureService.installFeature(feature);
        assertFeatureInstalled(feature);
    }

    protected void installAssertAndUninstallFeature(String... feature) throws Exception {
        Set<Feature> featuresBefore = new HashSet<Feature>(Arrays.asList(featureService.listInstalledFeatures()));
        try {
            for (String curFeature : feature) {
                featureService.installFeature(curFeature);
                assertFeatureInstalled(curFeature);
            }
        } finally {
            uninstallNewFeatures(featuresBefore);
        }
    }

    /**
     * The feature service does not uninstall feature dependencies when uninstalling a single feature. So we need to
     * make sure we uninstall all features that were newly installed.
     *
     * @param featuresBefore
     * @throws Exception
     */
    private void uninstallNewFeatures(Set<Feature> featuresBefore)
            throws Exception {
        Feature[] features = featureService.listInstalledFeatures();
        for (Feature curFeature : features) {
            if (!featuresBefore.contains(curFeature)) {
                try {
                    System.out.println("Uninstalling " + curFeature.getName());
                    featureService.uninstallFeature(curFeature.getName(), curFeature.getVersion());
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
