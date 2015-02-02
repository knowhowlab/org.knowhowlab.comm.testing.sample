package org.knowhowlab.comm.testing.sample.display.impl;

import org.apache.felix.scr.annotations.*;
import org.knowhowlab.comm.testing.sample.display.Display;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author dpishchukhin
 */
@Component(specVersion = "1.2", immediate = true, name = "display", label = "Sample Display", description = "Sample Display",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name = "Port", label = "Serial Port", description = "Serial Port"),
    @Property(name = "Baudrate", label = "Baudrate", description = "Baudrate", intValue = 9600),
    @Property(name = "Databits", label = "Databits", description = "Databits", intValue = 8),
    @Property(name = "Stopbits", label = "Stopbits", description = "Stopbits", intValue = 1),
    @Property(name = "Parity", label = "Parity", description = "Parity", intValue = 0)
})
@Service(Display.class)
public class DisplayComponent implements Display {
    private static final Logger logger = Logger.getLogger(DisplayComponent.class.getName());

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
}
