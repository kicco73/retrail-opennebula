/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

/**
 *
 * @author oneadmin
 */
public interface SemaphoreProtocol {

    public boolean setValue(boolean value) throws Exception;

    public boolean getValue() throws Exception;
}
