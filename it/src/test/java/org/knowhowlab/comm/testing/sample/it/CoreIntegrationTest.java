package org.knowhowlab.comm.testing.sample.it;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;
import org.knowhowlab.osgi.testing.assertions.BundleAssert;
import org.knowhowlab.osgi.testing.utils.BundleUtils;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.knowhowlab.osgi.testing.utils.cmpn.ConfigurationAdminUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.monitor.MonitorAdmin;
import org.osgi.service.monitor.Monitorable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knowhowlab.osgi.testing.assertions.ServiceAssert.assertServiceAvailable;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.and;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.create;
import static org.knowhowlab.osgi.testing.utils.FilterUtils.eq;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.vmOptions;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION;
import static org.osgi.framework.Constants.SERVICE_PID;

/**
 * @author dpishchukhin
 */
public class CoreIntegrationTest extends AbstractTest {
    private static MockRxTxDriver driver;

    @BeforeClass
    public static void initComm() throws IOException {
        driver = new MockRxTxDriver(createConfig());
        driver.initialize();
    }

    @After
    public void after() throws IOException {
        driver.reset();
    }

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        // scanner
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);

        // display
        PortConfig com3 = new PortConfig("COM3", PortType.SERIAL);
        ports.add(com3);
        PortConfig com4 = new PortConfig("COM4", PortType.SERIAL, com3);
        ports.add(com4);
        return new DriverConfig(ports);
    }

    @Configuration
    public static Option[] customTestConfiguration() {
        Option[] options = options(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.8.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("1.8.2"),
                mavenBundle().groupId("org.knowhowlab.comm").artifactId("org.knowhowlab.comm.testing.rxtx").version("0.1"),
                mavenBundle().groupId("org.knowhowlab.comm.sample").artifactId("core").version(System.getProperty("project.version")),
                mavenBundle().groupId("org.knowhowlab.comm.sample").artifactId("display-driver").version(System.getProperty("project.version")),
                mavenBundle().groupId("org.knowhowlab.comm.sample").artifactId("barcode-scanner-driver").version(System.getProperty("project.version"))
                
                //,vmOptions("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        );
        return combine(options, baseConfiguration());
    }

    @Test
    public void testCore() throws Exception {
        // scanner config
        ConfigurationAdminUtils.supplyConfiguration(bc, "scanner", null, new HashMap() {{
            put(".port", "COM1");
        }}, 0);
        // display config
        ConfigurationAdminUtils.supplyConfiguration(bc, "display", null, new HashMap() {{
            put(".port", "COM3");
        }}, 0);

        TimeUnit.MILLISECONDS.sleep(500);

        // write to scanner port
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        CommPort com2 = com2Id.open("Test", 2000);
        com2.getOutputStream().write("1234567890".getBytes(Charset.defaultCharset()));

        CommPortIdentifier com4Id = CommPortIdentifier.getPortIdentifier("COM4");
        CommPort com4 = com4Id.open("Test", 2000);
        byte[] buff = new byte[32];
        int read = com4.getInputStream().read(buff);

        assertThat(Arrays.copyOf(buff, read), is(equalTo("1234567890".getBytes(Charset.defaultCharset()))));

        com2.close();
        com4.close();
    }
}
