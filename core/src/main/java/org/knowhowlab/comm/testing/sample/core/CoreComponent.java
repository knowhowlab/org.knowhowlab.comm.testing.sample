package org.knowhowlab.comm.testing.sample.core;

/**
 * @author dpishchukhin
 */
import org.apache.felix.scr.annotations.*;
import org.knowhowlab.comm.testing.sample.display.Display;
import org.knowhowlab.comm.testing.sample.scanner.BarcodeListener;

import java.nio.charset.Charset;
import java.util.logging.Logger;

@Component(specVersion = "1.2", immediate = true, name = "core", label = "Sample Core", description = "Sample Core",
        metatype = false, policy = ConfigurationPolicy.IGNORE)
@Service(BarcodeListener.class)
public class CoreComponent implements BarcodeListener {
    private static final Logger LOG = Logger.getLogger(CoreComponent.class.getName());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.STATIC)
    private Display display;

    @Activate
    public void activate() {
        display.println("Cash desk");
        display.println("Welcome!");
        LOG.info("ACTIVATED");
    }

    @Deactivate
    public void deactivate() {
        display.println("Cash desk");
        display.println("Bye!");
        LOG.info("DEACTIVATED");
    }

    @Override
    public void scanned(byte[] code) {
        String barcode = new String(code, Charset.defaultCharset());
        LOG.info(String.format("Data scanned: %s", barcode));

        display.println(barcode);
    }
}
