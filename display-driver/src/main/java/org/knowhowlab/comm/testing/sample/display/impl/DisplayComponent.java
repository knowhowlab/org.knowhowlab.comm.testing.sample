package org.knowhowlab.comm.testing.sample.display.impl;

import gnu.io.*;
import org.apache.felix.scr.annotations.*;
import org.knowhowlab.comm.testing.sample.display.Display;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "display", label = "Sample Display", description = "Sample Display",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = ".port", label = "Serial Port", description = "Serial Port"),
    @Property(name = ".baudrate", label = "Baudrate", description = "Baudrate", intValue = 9600),
    @Property(name = ".databits", label = "Databits", description = "Databits", intValue = 8),
    @Property(name = ".stopbits", label = "Stopbits", description = "Stopbits", intValue = 1),
    @Property(name = ".parity", label = "Parity", description = "Parity", intValue = 0)
})
@Service(Display.class)
public class DisplayComponent implements Display {
    private static final Logger LOG = Logger.getLogger(DisplayComponent.class.getName());

    private static final String PORT_CONFIG_PROP = ".port";
    private static final String BAUDRATE_CONFIG_PROP = ".baudrate";
    private static final String DATABITS_CONFIG_PROP = ".databits";
    private static final String STOPBITS_CONFIG_PROP = ".stopbits";
    private static final String PARITY_CONFIG_PROP = ".parity";

    private String port;
    private int baudrate;
    private int databits;
    private int stopbits;
    private int parity;

    private SerialPort serialPort;

    @Activate
    public void activate(Map<String, Object> properties) {
        readConfig(properties);

        openConnectionToDevice();

        LOG.info("ACTIVATED");
    }

    @Deactivate
    public void deactivate() {
        if (isPortOpened()) {
            closeConnectionToDevice();
        }

        LOG.info("DEACTIVATED");
    }

    @Modified
    public void modified(Map<String, Object> properties) {
        if (readConfig(properties)) {
            reopenPort();
        }
        LOG.info("MODIFIED");
    }

    @Override
    public void println(String text) {
        write(String.format("%s\r\n", text).getBytes(Charset.defaultCharset()));
    }
    @Override
    public void print(String text) {
        write(text.getBytes(Charset.defaultCharset()));
    }

    private void write(byte[] data) {
        try {
            serialPort.getOutputStream().write(data);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to write data", e);
        }
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
            serialPort.close();
            serialPort = null;
        }
        LOG.info(String.format("Port %s closed", port));
    }

    private boolean openConnectionToDevice() {
        if (openPort()) {
            LOG.info(String.format("Port %s opened", port));
            return true;
        } else {
            LOG.warning(String.format("Unable to set connection to port %s", port));
            closeConnectionToDevice();
            return false;
        }
    }

    private boolean openPort() {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
            if (portIdentifier.getPortType() != CommPortIdentifier.PORT_SERIAL) {
                LOG.warning(String.format("%s port is not SERIAL port", port));
                return false;
            }
            serialPort = (SerialPort) portIdentifier.open("Display Driver", 2000);
            serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
            return true;
        } catch (PortInUseException e) {
            LOG.log(Level.WARNING, String.format("%s port is in use", port), e);
        } catch (NoSuchPortException e) {
            LOG.log(Level.WARNING, String.format("%s port is unknown", port), e);
        } catch (UnsupportedCommOperationException e) {
            LOG.log(Level.WARNING, String.format("Unable to set connection parameters to %s port: %d, %d, %d, %d", port, baudrate, databits, stopbits, parity), e);
        }
        return false;
    }

    private boolean readConfig(Map<String, Object> properties) {
        // 1. read new connection settings
        boolean newValues = false;
        Object portProp = properties.get(PORT_CONFIG_PROP);
        if (portProp != null && (portProp instanceof String) && !portProp.equals(port)) {
            port = (String) portProp;
            newValues = true;
        }
        Object baudrateProp = properties.get(BAUDRATE_CONFIG_PROP);
        if (baudrateProp != null && (baudrateProp instanceof Integer) && !baudrateProp.equals(baudrate)) {
            baudrate = (Integer) baudrateProp;
            newValues = true;
        }
        Object databitsProp = properties.get(DATABITS_CONFIG_PROP);
        if (databitsProp != null && (databitsProp instanceof Integer) && !databitsProp.equals(databits)) {
            databits = (Integer) databitsProp;
            newValues = true;
        }
        Object stopbitsProp = properties.get(STOPBITS_CONFIG_PROP);
        if (stopbitsProp != null && (stopbitsProp instanceof Integer) && !stopbitsProp.equals(stopbits)) {
            stopbits = (Integer) stopbitsProp;
            newValues = true;
        }
        Object parityProp = properties.get(PARITY_CONFIG_PROP);
        if (parityProp != null && (parityProp instanceof Integer) && !parityProp.equals(parity)) {
            parity = (Integer) parityProp;
            newValues = true;
        }

        return newValues;
    }
}
