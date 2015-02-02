package org.knowhowlab.comm.testing.sample.scanner;

/**
 * @author dpishchukhin
 */
public interface BarcodeListener {
    void scanned(byte[] code);
}
