package org.knowhowlab.comm.testing.sample.display.impl;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class DisplayComponentTest {
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
    public void testPrint() throws Exception {
        DisplayComponent displayComponent = new DisplayComponent();
        displayComponent.activate(new HashMap<String, Object>() {{
            put(".port", "COM1");
        }});

        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        CommPort com2 = com2Id.open("Test", 2000);
        
        displayComponent.print("Hello World!");

        byte[] buff = new byte[32];
        int read = com2.getInputStream().read(buff);
        assertThat(new String(buff, 0, read), is(equalTo("Hello World!\r\n")));
        com2.close();

        displayComponent.deactivate();
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