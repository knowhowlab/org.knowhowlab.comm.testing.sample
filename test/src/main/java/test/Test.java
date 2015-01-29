package test;

import gnu.io.CommPortIdentifier;

import java.util.Enumeration;

/**
 * @author dpishchukhin
 */
public class Test {
    public static void main(String[] args) {
        Enumeration identifiers = CommPortIdentifier.getPortIdentifiers();
        while (identifiers.hasMoreElements()) {
            CommPortIdentifier identifier = (CommPortIdentifier) identifiers.nextElement();
            System.out.println(identifier.getName());
        }
    }
}
