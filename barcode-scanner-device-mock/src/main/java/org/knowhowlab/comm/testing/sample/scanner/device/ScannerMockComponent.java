package org.knowhowlab.comm.testing.sample.scanner.device;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "scanner-mock", label = "Barcode Scanner Mock", description = "Barcode Scanner Mock",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = ".device-port", label = "Device Serial Port", description = "Serial Port", value = "COM1"),
        @Property(name = ".driver-port", label = "Driver Serial Port", description = "Serial Port"),
        @Property(name = CommandProcessor.COMMAND_SCOPE, value = "scanner"),
        @Property(name = CommandProcessor.COMMAND_FUNCTION, value = {"scan"})
})
@Service(ScannerMockComponent.class)
public class ScannerMockComponent {
    private static final Logger LOG = Logger.getLogger(ScannerMockComponent.class.getName());

    private static final String DEVICE_PORT_CONFIG_PROP = ".device-port";
    private static final String DRIVER_PORT_CONFIG_PROP = ".driver-port";

    private String devicePort;
    private String driverPort;

    private SerialPort serialPort;

    @Activate
    public void activate(Map<String, Object> properties) {
        readConfig(properties);

        activateMockDriver();

        openConnectionToDevice();

        LOG.info("ACTIVATED");
    }

    private void activateMockDriver() {
        MockRxTxDriver driver = new MockRxTxDriver(createConfig());
        driver.initialize();
    }

    private DriverConfig createConfig() {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig(devicePort, PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig(driverPort, PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }

    @Deactivate
    public void deactivate() {
        if (isPortOpened()) {
            closeConnectionToDevice();
        }

        LOG.info("DEACTIVATED");
    }

    @Descriptor("Scan barcode")
    public void scan(@Descriptor("Barcode") String barcode) {
        LOG.info(String.format("Scanned barcode: %s", barcode));
        try {
            serialPort.getOutputStream().write(barcode.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to send barcode", e);
        }
    }

    private boolean isPortOpened() {
        return serialPort != null;
    }

    private void closeConnectionToDevice() {
        if (serialPort != null) {
            serialPort.notifyOnDataAvailable(false);
            serialPort.removeEventListener();
            serialPort.close();
            serialPort = null;
        }
        LOG.info(String.format("Port %s closed", devicePort));
    }

    private boolean openConnectionToDevice() {
        if (openPort()) {
            LOG.info(String.format("Port %s opened", devicePort));
            return true;
        } else {
            LOG.warning(String.format("Unable to set connection to port %s", devicePort));
            closeConnectionToDevice();
            return false;
        }
    }

    private boolean openPort() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(devicePort);
            if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                LOG.warning(String.format("%s port is not SERIAL port", devicePort));
                return false;
            }
            serialPort = (SerialPort) portIdentifier.open("Scanner Device Mock", 2000);
            return true;
        } catch (PortInUseException e) {
            LOG.log(Level.WARNING, String.format("%s port is in use", devicePort), e);
        } catch (NoSuchPortException e) {
            LOG.log(Level.WARNING, String.format("%s port is unknown", devicePort), e);
        }
        return false;
    }

    private boolean readConfig(Map<String, Object> properties) {
        // 1. read new connection settings
        boolean newValues = false;
        Object devicePortProp = properties.get(DEVICE_PORT_CONFIG_PROP);
        if (devicePortProp != null && (devicePortProp instanceof String) && !devicePortProp.equals(devicePort)) {
            devicePort = (String) devicePortProp;
            newValues = true;
        }
        Object driverPortProp = properties.get(DRIVER_PORT_CONFIG_PROP);
        if (driverPortProp != null && (driverPortProp instanceof String) && !driverPortProp.equals(driverPort)) {
            driverPort = (String) driverPortProp;
            newValues = true;
        }

        return newValues;
    }
}
