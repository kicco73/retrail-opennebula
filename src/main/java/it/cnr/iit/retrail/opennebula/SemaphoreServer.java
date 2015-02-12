/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.commons.Server;
import java.io.InputStream;
import java.net.URL;

/**
 *
 * @author kicco
 */
public class SemaphoreServer extends Server implements SemaphoreProtocol {
    protected boolean green = true;
    static final String defaultKeystoreName = "/META-INF/keystore.jks";
    static final String defaultKeystorePassword = "uconas4wc";
    
    public SemaphoreServer(URL myUrl, boolean green) throws Exception {
        super(myUrl, SemaphoreServerProxy.class, SemaphoreServer.class.getSimpleName());
        log.warn("creating semaphore server at URL: {}, initial value: {}; namespace: {}", myUrl, green, getClass().getSimpleName());
        // Telling server to use a self-signed certificate and
        // trust any client.
        InputStream ks = SemaphoreServer.class.getResourceAsStream(defaultKeystoreName);
        trustAllPeers(ks, defaultKeystorePassword);
        this.green = green;
    }

    @Override
    public synchronized boolean setValue(boolean green) throws Exception {
        log.warn("setting new value: {}", green);
        this.green = green;
        return this.green;
    }
    
    @Override
    public synchronized boolean getValue() {
        return green;
    }
}
