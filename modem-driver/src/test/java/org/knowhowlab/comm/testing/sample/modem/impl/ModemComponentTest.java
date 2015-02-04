package org.knowhowlab.comm.testing.sample.modem.impl;

import gnu.io.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ModemComponentTest {
    private static MockRxTxDriver driver;

    @BeforeClass
    public static void init() throws IOException {
        driver = new MockRxTxDriver(createConfig());
        driver.initialize();
    }

    @After
    public void after() throws IOException {
        driver.reset();
    }

    @Test
    public void testInit() throws Exception {
        ModemComponent modemComponent = new ModemComponent();

        final String[] requests = new String[1];
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        final SerialPort com2 = (SerialPort) com2Id.open("Test", 2000);
        com2.addEventListener(new SerialPortEventListener() {
            int i = 0;
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                try {
                    switch (serialPortEvent.getEventType()) {
                        case SerialPortEvent.DATA_AVAILABLE:
                            byte[] buff = new byte[32];
                            int read = com2.getInputStream().read(buff);
                            requests[i] = new String(buff, 0, read, Charset.defaultCharset());

                            switch (i) {
                                case 0:
                                    com2.getOutputStream().write("OK\r".getBytes(Charset.defaultCharset()));
                                    break;
                            }
                            i++;
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        com2.notifyOnDataAvailable(true);

        modemComponent.activate(new HashMap<String, Object>() {{
            put(".port", "COM1");
        }});

        assertThat(requests[0], is(equalTo("AT+CMGF=1\r")));

        com2.close();
        modemComponent.deactivate();
    }

    @Test
    public void testSendSMS_OK() throws Exception {
        ModemComponent modemComponent = new ModemComponent();

        final String[] requests = new String[2];
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        final SerialPort com2 = (SerialPort) com2Id.open("Test", 2000);
        com2.addEventListener(new SerialPortEventListener() {
            int i = 0;
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                try {
                    switch (serialPortEvent.getEventType()) {
                        case SerialPortEvent.DATA_AVAILABLE:
                            byte[] buff = new byte[128];
                            int read = com2.getInputStream().read(buff);
                            requests[i] = new String(buff, 0, read, Charset.defaultCharset());

                            switch (i) {
                                case 0:
                                    com2.getOutputStream().write("OK\r".getBytes(Charset.defaultCharset()));
                                    break;
                                case 1:
                                    com2.getOutputStream().write("+CMGS: 1\rOK\r".getBytes(Charset.defaultCharset()));
                                    break;
                            }
                            i++;
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        com2.notifyOnDataAvailable(true);

        modemComponent.activate(new HashMap<String, Object>() {{
            put(".port", "COM1");
            put(".number", "+123456");
        }});

        assertThat(modemComponent.sendSMS("test"), is(equalTo(true)));

        assertThat(requests[0], is(equalTo("AT+CMGF=1\r")));
        assertThat(requests[1], is(equalTo("AT+CMGS=\"+123456\"\rtest\u001A")));

        com2.close();
        modemComponent.deactivate();
    }

    @Test
    public void testSendSMS_ERROR() throws Exception {
        ModemComponent modemComponent = new ModemComponent();

        final String[] requests = new String[2];
        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        final SerialPort com2 = (SerialPort) com2Id.open("Test", 2000);
        com2.addEventListener(new SerialPortEventListener() {
            int i = 0;
            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                try {
                    switch (serialPortEvent.getEventType()) {
                        case SerialPortEvent.DATA_AVAILABLE:
                            byte[] buff = new byte[128];
                            int read = com2.getInputStream().read(buff);
                            requests[i] = new String(buff, 0, read, Charset.defaultCharset());

                            switch (i) {
                                case 0:
                                    com2.getOutputStream().write("OK\r".getBytes(Charset.defaultCharset()));
                                    break;
                                case 1:
                                    com2.getOutputStream().write("ERROR\r".getBytes(Charset.defaultCharset()));
                                    break;
                            }
                            i++;
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        com2.notifyOnDataAvailable(true);

        modemComponent.activate(new HashMap<String, Object>() {{
            put(".port", "COM1");
            put(".number", "+123456");
        }});

        assertThat(modemComponent.sendSMS("test"), is(equalTo(false)));

        assertThat(requests[0], is(equalTo("AT+CMGF=1\r")));
        assertThat(requests[1], is(equalTo("AT+CMGS=\"+123456\"\rtest\u001A")));

        com2.close();
        modemComponent.deactivate();
    }

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }
}