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

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class CellarTestSupport {
    static final Long COMMAND_TIMEOUT = 10000L;
    static final Long DEFAULT_TIMEOUT = 10000L;
    static final Long SERVICE_TIMEOUT = 30000L;
    static final String KARAF_VERSION = "3.0.0-SNAPSHOT";
    public static final String RMI_SERVER_PORT = "44445";
    public static final String HTTP_PORT = "9081";
    public static final String RMI_REG_PORT = "1100";
    static final String GROUP_ID = "org.apache.karaf";
    static final String ARTIFACT_ID = "apache-karaf";
    static final String INSTANCE_STARTED = "Started";
    static final String INSTANCE_STARTING = "Starting";
    static final String DEBUG_OPTS = " --java-opts \"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s\"";
    protected String cellarFeatureURI;
    ExecutorService executor = Executors.newCachedThreadPool();
    @Inject
    protected BundleContext bundleContext;
    @Inject
    protected FeaturesService featureService;
    @Inject
    BootFinished bootFinished;

    /**
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    /**
     * Installs the Cellar feature
     */
    protected void installCellar() {
        try {
            cellarFeatureURI = maven().groupId("org.apache.karaf.cellar").artifactId("apache-karaf-cellar").version(KARAF_VERSION).classifier("features").type("xml").getURL();
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
     */
    protected void createCellarChild(String name) {
        createCellarChild(name, false, 0);
    }

    protected void createCellarChild(String name, boolean debug, int port) {
        int instances = 0;
        String createCommand = "instance:create --featureURL " + cellarFeatureURI + " --feature cellar ";
        if (debug && port > 0) {
            createCommand = createCommand + String.format(DEBUG_OPTS, port);
        }
        System.err.println(executeCommand(createCommand + " " + name));
        System.err.println(executeCommand("instance:start " + name));

        //Wait till the node is listed as Starting
        System.err.print("Waiting for " + name + " to start ");
        for (int i = 0; i < 5 && instances == 0; i++) {
            String response = executeCommand("instance:list | grep " + name + " | grep -c " + INSTANCE_STARTED, COMMAND_TIMEOUT, false);
            instances = Integer.parseInt(response.trim());
            System.err.print(".");
            try {
                Thread.sleep(3000);
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
        System.err.println(executeCommand("instance:connect -u karaf -p karaf " + name + " feature:uninstall cellar"));
        System.err.println(executeCommand("instance:stop " + name));
    }

    /**
     * Returns the node id of a specific child instance.
     */
    protected String getNodeIdOfChild(String name) {
        String node;
        String nodesList = executeCommand("instance:connect -u karaf -p karaf " + name + " cluster:node-list | grep \\\\*", COMMAND_TIMEOUT, false);
        System.err.println("Get node id of child response: " + nodesList);
        int stop = nodesList.indexOf(']');
        node = nodesList.substring(0, stop);
        int start = node.lastIndexOf('[');
        node = node.substring(start + 1);
        node = node.trim();
        return node;
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").version(KARAF_VERSION).type("tar.gz");
        Option[] options = new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
            logLevel(LogLevelOption.LogLevel.INFO),
            keepRuntimeFolder(),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "config,standard,region,package,kar,ssh,management"),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", RMI_REG_PORT),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", RMI_SERVER_PORT),
            //            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j.logger.org.apache.aries.blueprint", "DEBUG"),
            //            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j.logger.org.apache.karaf.cellar", "DEBUG"),
            editConfigurationFilePut("etc/org.ops4j.pax.logging.cfg", "log4j.logger.org.apache.karaf.cellar.shell", "WARN"), };
        String debug = System.getProperty("debugMain");
        if (debug != null) {
            int l = options.length;
            options = Arrays.copyOf(options, l + 1);
            options[l] = KarafDistributionOption.debugConfiguration();
        }
        return options;
    }

    protected boolean waitForInstanceToCluster(final int desiredTotal) {
        return waitForInstanceToCluster(desiredTotal, 15000L);
    }

    protected boolean waitForInstanceToCluster(final int desiredTotal, final Long timeout) {
        final FutureTask<Boolean> commandFuture = new FutureTask<Boolean>(
                new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                ClusterManager manager = CellarTestSupport.this.getOsgiService(ClusterManager.class, timeout);
                boolean found = false;
                while (!found) {
                    Set<Node> nodes = manager.listNodes();
                    if (nodes.size() >= desiredTotal) {
                        return true;
                    }
                    Thread.sleep(1000);
                    if (Thread.interrupted()) {
                        return false;
                    }
                }
                return false;
            }
        });

        try {
            executor.submit(commandFuture);
            boolean result = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
            System.out.println("Total nodes found successfully: " + desiredTotal);
            return result;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
        return executeCommand(command, COMMAND_TIMEOUT, false);
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command The command to execute.
     * @param timeout The amount of time in millis to wait for the command to execute.
     * @param silent Specifies if the command should be displayed in the screen.
     * @return
     */
    protected String executeCommand(final String command, final Long timeout, final Boolean silent) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
        FutureTask<String> commandFuture = new FutureTask<String>(
                new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    if (!silent) {
                        System.err.println(command);
                    }
                    commandSession.execute(command);
                } finally {
                    printStream.flush();
                }
                return byteArrayOutputStream.toString();
            }
        });

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT for command: " + command;
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

    /**
     * Explodes the dictionary into a ,-delimited list of key=value pairs
     */
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuilder result = new StringBuilder();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
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
                Dictionary dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref);
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref);
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds a free port starting from the give port numner.
     *
     * @return
     */
    protected int getFreePort(int port) {
        while (!isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    /**
     * Returns true if port is available for use.
     *
     * @param port
     * @return
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    /*
     * Provides an iterable collection of references, even if the original array is null
     */
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
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
        Assert.assertTrue("Bundle " + name + " should be installed", isBundleInstalled(name));
    }

    protected void assertBundleNotInstalled(String name) {
        Assert.assertFalse("Bundle " + name + " should not be installed", isBundleInstalled(name));
    }

    private boolean isBundleInstalled(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return true;
            }
        }
        return false;
    }
}
