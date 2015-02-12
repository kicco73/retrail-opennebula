/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

/**
 *
 * @author oneadmin
 */
public class SemaphoreServerProxy implements SemaphoreProtocol {

    @Override
    public boolean setValue(boolean value) throws Exception {
        return Main.semaphoreServer.setValue(value);
    }

    @Override
    public boolean getValue() {
        return Main.semaphoreServer.getValue();
    }

}
