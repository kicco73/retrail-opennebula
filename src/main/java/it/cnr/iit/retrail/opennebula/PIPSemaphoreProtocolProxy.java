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
        return PIPSemaphore.getInstance().setValue(value);
    }

    @Override
    public boolean getValue() throws Exception {
        return PIPSemaphore.getInstance().getValue();
    }

    @Override
    public boolean setPolling(boolean value) throws Exception {
        return PIPSemaphore.getInstance().setPolling(value);
    }

    @Override
    public boolean isPolling() {
        return PIPSemaphore.getInstance().isPolling();
    }

    @Override
    public int setWatchdogPeriod(int newWatchdogPeriod) {
        return PIPSemaphore.getInstance().setWatchdogPeriod(newWatchdogPeriod);
    }

    @Override
    public int getWatchdogPeriod() {
        return PIPSemaphore.getInstance().getWatchdogPeriod();
    }
}
