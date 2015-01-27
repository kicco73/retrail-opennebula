/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

/**
 *
 * @author oneadmin
 */
public class PIPSemaphoreProtocol {

    public boolean setValue(boolean value) throws Exception {
        Main.pipSemaphore.setValue(value);
        return getValue();
    }

    public boolean getValue() {
        return Main.pipSemaphore.getValue();
    }
}
