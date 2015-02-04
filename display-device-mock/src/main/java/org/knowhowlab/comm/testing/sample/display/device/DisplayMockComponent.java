package org.knowhowlab.comm.testing.sample.display.device;

import gnu.io.*;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "display-mock", label = "Display Mock", description = "Display Mock",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = ".device-port", label = "Device Serial Port", description = "Serial Port", value = "COM3"),
        @Property(name = ".driver-port", label = "Driver Serial Port", description = "Serial Port")
})
public class DisplayMockComponent {
    private static final Logger LOG = Logger.getLogger(DisplayMockComponent.class.getName());

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

    private boolean isPortOpened() {
        return serialPort != null;
    }

    private boolean reopenPort() {
        if (isPortOpened()) {
            // 2. try to close opened port
            closeConnectionToDevice();
        }
        // 3. try to open port
        return openConnectionToDevice();
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
        if (openPort() && addDataListener()) {
            LOG.info(String.format("Port %s opened", devicePort));
            return true;
        } else {
            LOG.warning(String.format("Unable to set connection to port %s", devicePort));
            closeConnectionToDevice();
            return false;
        }
    }

    private boolean addDataListener() {
        try {
            serialPort.addEventListener(new DataListener());
            serialPort.notifyOnDataAvailable(true);
            return true;
        } catch (TooManyListenersException e) {
            throw new RuntimeException("Listener is already added", e);
        }
    }

    private boolean openPort() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(devicePort);
            if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                LOG.warning(String.format("%s port is not SERIAL port", devicePort));
                return false;
            }
            serialPort = (SerialPort) portIdentifier.open("Display Device Mock", 2000);
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

    private void printOnDisplay(byte[] bytes) {
        System.out.println(String.format("Display: %s", new String(bytes, Charset.defaultCharset())));
    }

    private class DataListener implements SerialPortEventListener {
        private final StringBuilder buff = new StringBuilder();

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (SerialPortEvent.DATA_AVAILABLE == serialPortEvent.getEventType()) {
                try {
                    int read;
                    byte[] byteBuff = new byte[128];
                    InputStream inputStream = new BufferedInputStream(serialPort.getInputStream());
                    while (inputStream.available() > 0) {
                        if ((read = inputStream.read(byteBuff)) == -1) {
                            break;
                        }
                        printOnDisplay(Arrays.copyOf(byteBuff, read));
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, String.format("Read error from port %s", serialPort), e);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, String.format("Read error from port %s", serialPort), e);
                }
            }
        }
    }
}
