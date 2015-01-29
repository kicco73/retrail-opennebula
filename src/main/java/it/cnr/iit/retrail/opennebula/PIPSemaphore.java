/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.server.pip.impl.StandAlonePIP;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.webserver.WebServer;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPSemaphore extends StandAlonePIP {
    protected boolean green = true;
    final public String id = "Semaphore";
    final public String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    final WebServer webServer;
    final URL url;

    public PIPSemaphore(URL myUrl, boolean green) throws UnknownHostException, XmlRpcException, NoSuchAlgorithmException, KeyManagementException {
        super();
        url = myUrl;
        log.warn("creating semaphore at URL: {}, initial {} value: {}; namespace: {}", myUrl, id, green, getClass().getSimpleName());
        this.log = LoggerFactory.getLogger(PIPSemaphore.class);
        this.green = green;
        this.webServer = Server.createWebServer(myUrl, PIPSemaphoreProtocol.class, getClass().getSimpleName());
    }
    
    public synchronized void setValue(boolean green) throws Exception {
        this.green = green;
        log.warn("setting new value: {}, attributes {}", green);
        //notifyChanges(listManagedAttributes());
        // XXX FIXME Workaround
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#boolean", Boolean.toString(green), "http://localhost:8080/federation-id-prov/saml", category);
        notifyChanges(test);
    }
    
    public synchronized boolean getValue() {
        return green;
    }
    
    @Override
    public void onBeforeTryAccess(PepRequestInterface request) {
        log.warn("inserting attribute {}: {}", id, green);
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#boolean", Boolean.toString(green), "http://localhost:8080/federation-id-prov/saml", category);
        request.add(test);
    }
    
    @Override
    public void run() {
    }
    
    @Override
    public void init() {
        super.init();
        try {
            // start server
            webServer.start();
        } catch (IOException ex) {
            log.error("while starting webserver: {}", ex);
        }
    }
    
    @Override
    public void term() {
        webServer.shutdown();
        super.term();
    }

}
