package org.knowhowlab.comm.testing.sample.modem.impl;

import gnu.io.*;
import org.apache.felix.scr.annotations.*;
import org.knowhowlab.comm.testing.sample.modem.Modem;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "modem", label = "Sample Modem", description = "Sample Modem",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = ".port", label = "Serial Port", description = "Serial Port"),
        @Property(name = ".baudrate", label = "Baudrate", description = "Baudrate", intValue = 9600),
        @Property(name = ".databits", label = "Databits", description = "Databits", intValue = 8),
        @Property(name = ".stopbits", label = "Stopbits", description = "Stopbits", intValue = 1),
        @Property(name = ".parity", label = "Parity", description = "Parity", intValue = 0),
        @Property(name = ".number", label = "Delivery Number", description = "Delivery Number")
})
@Service(Modem.class)
public class ModemComponent implements Modem {
    private static final Logger LOG = Logger.getLogger(ModemComponent.class.getName());

    private static final String PORT_CONFIG_PROP = ".port";
    private static final String BAUDRATE_CONFIG_PROP = ".baudrate";
    private static final String DATABITS_CONFIG_PROP = ".databits";
    private static final String STOPBITS_CONFIG_PROP = ".stopbits";
    private static final String PARITY_CONFIG_PROP = ".parity";
    private static final String NUMBER_CONFIG_PROP = ".number";

    private static final String CMGF_COMMAND = "AT+CMGF=1\r";
    private static final String CMGS_COMMAND_TEMPLATE = "AT+CMGS=\"%s\"\r%s\u001A";
    private static final String CMGS_ANSWER_TEMPLATE = "+CMGS: {1}\r{0}";
    private static final String OK_ANSWER = "OK";
    private static final String DEFAULT_ANSWER_TEMPLATE = "{0}";
    private static final String ERROR_ANSWER = "ERROR";

    private String port;
    private int baudrate;
    private int databits;
    private int stopbits;
    private int parity;
    private String number;

    private SerialPort serialPort;

    @Activate
    public void activate(Map<String, Object> properties) {
        readConfig(properties);

        openConnectionToDevice();

        if (sendCommand(CMGF_COMMAND, DEFAULT_ANSWER_TEMPLATE)) {
            LOG.info("Modem is connected");
        } else {
            LOG.warning("Modem is not connected");
        };

        LOG.info("ACTIVATED");
    }

    private boolean sendCommand(String command, String answerTemplate) {
        LOG.info(String.format("-> %s", command));

        write(String.format("%s", command).getBytes(Charset.defaultCharset()));

        String answer = readAnswer();
        LOG.info(String.format("<- %s", answer));
        try {
            return OK_ANSWER.equals(new MessageFormat(answerTemplate).parse(answer)[0]);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unexpected answer: " + answer);
            return false;
        }
    }

    private String readAnswer() {
        byte[] buff = new byte[32];
        int read;
        try {
            read = serialPort.getInputStream().read(buff);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Read error", e);
            return ERROR_ANSWER;
        }
        if (read > 0) {
            return new String(buff, 0, read, Charset.defaultCharset()).trim();
        } else {
            LOG.warning("No answer");
            return ERROR_ANSWER;
        }
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
    public boolean sendSMS(String text) {
        return sendCommand(String.format(CMGS_COMMAND_TEMPLATE, number, text), CMGS_ANSWER_TEMPLATE);
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
        Object numberProp = properties.get(NUMBER_CONFIG_PROP);
        if (numberProp != null && (numberProp instanceof String) && !numberProp.equals(number)) {
            number = (String) numberProp;
            newValues = true;
        }

        return newValues;
    }
}
