/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

/**
 *
 * @author oneadmin
 */
public class PIPSemaphoreProtocolProxy implements PIPSemaphoreProtocol {

    @Override
    public boolean setValue(boolean value) throws Exception {
        return Main.pipSemaphore.setValue(value);
    }

    @Override
    public boolean getValue() throws Exception {
        return Main.pipSemaphore.getValue();
    }

    @Override
    public boolean setPolling(boolean value) throws Exception {
        return Main.pipSemaphore.setPolling(value);
    }

    @Override
    public boolean isPolling() {
        return Main.pipSemaphore.isPolling();
    }

    @Override
    public int setWatchdogPeriod(int newWatchdogPeriod) {
        return Main.pipSemaphore.setWatchdogPeriod(newWatchdogPeriod);
    }

    @Override
    public int getWatchdogPeriod() {
        return Main.pipSemaphore.getWatchdogPeriod();
    }
}
