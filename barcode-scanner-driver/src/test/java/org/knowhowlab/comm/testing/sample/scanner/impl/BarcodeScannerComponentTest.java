package org.knowhowlab.comm.testing.sample.scanner.impl;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowhowlab.comm.testing.common.config.DriverConfig;
import org.knowhowlab.comm.testing.common.config.PortConfig;
import org.knowhowlab.comm.testing.common.config.PortType;
import org.knowhowlab.comm.testing.rxtx.MockRxTxDriver;
import org.knowhowlab.comm.testing.sample.scanner.BarcodeListener;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BarcodeScannerComponentTest {
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
    public void testScanCode() throws Exception {
        BarcodeScannerComponent scannerComponent = new BarcodeScannerComponent();
        scannerComponent.activate(new HashMap<String, Object>() {{
            put(".port", "COM1");
        }});
        TestBarcodeListener listener = new TestBarcodeListener();
        scannerComponent.bindListener(listener);

        CommPortIdentifier com2Id = CommPortIdentifier.getPortIdentifier("COM2");
        CommPort com2 = com2Id.open("Test", 2000);

        com2.getOutputStream().write("1234567890\r".getBytes(Charset.defaultCharset()));

        assertThat(listener.getLatestCode(), is(equalTo("1234567890".getBytes(Charset.defaultCharset()))));

        com2.close();

        scannerComponent.deactivate();
    }

    private static DriverConfig createConfig() throws IOException {
        List<PortConfig> ports = new ArrayList<PortConfig>();
        PortConfig com1 = new PortConfig("COM1", PortType.SERIAL);
        ports.add(com1);
        PortConfig com2 = new PortConfig("COM2", PortType.SERIAL, com1);
        ports.add(com2);
        return new DriverConfig(ports);
    }

    private static class TestBarcodeListener implements BarcodeListener {
        private byte[] latestCode;
        @Override
        public void scanned(byte[] code) {
            latestCode = code;
        }

        public byte[] getLatestCode() {
            return latestCode;
        }
    }
}