package org.knowhowlab.comm.testing.sample.scanner.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.knowhowlab.comm.testing.sample.scanner.BarcodeListener;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "scanner", label = "Sample Scanner", description = "Sample Scanner",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = "Port", label = "Serial Port", description = "Serial Port"),
        @Property(name = "Baudrate", label = "Baudrate", description = "Baudrate", intValue = 9600),
        @Property(name = "Databits", label = "Databits", description = "Databits", intValue = 8),
        @Property(name = "Stopbits", label = "Stopbits", description = "Stopbits", intValue = 1),
        @Property(name = "Parity", label = "Parity", description = "Parity", intValue = 0)
})
public class BarcodeScannerComponent {
    private static final Logger logger = Logger.getLogger(BarcodeScannerComponent.class.getName());

    @Reference(referenceInterface = BarcodeListener.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC,
            bind = "bindListener", unbind = "unbindListener")
    private Set<BarcodeListener> listeners = new HashSet<BarcodeListener>();

    @Activate
    public void activate(Map<String, Object> properties) {
        logger.info("ACTIVATED");
    }

    @Deactivate
    public void deactivate() {
        logger.info("DEACTIVATED");
    }

    @Modified
    public void modified(Map<String, Object> properties) {
        logger.info("MODIFIED");
    }

    protected void bindListener(BarcodeListener listener) {
        listeners.add(listener);
    }

    protected void unbindListener(BarcodeListener listener) {
        listeners.remove(listener);
    }
}
