package it.cnr.iit.retrail.opennebula;

/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

import it.cnr.iit.retrail.opennebula.*;

/**
 *
 * @author oneadmin
 */
public interface PIPSemaphoreProtocol extends SemaphoreProtocol {

    public boolean setPolling(boolean value) throws Exception;

    public boolean isPolling();
    
    public int setWatchdogPeriod(int newWatchdogPeriod);

    public int getWatchdogPeriod();
}
