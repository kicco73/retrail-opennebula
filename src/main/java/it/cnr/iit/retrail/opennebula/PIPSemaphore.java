/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import it.cnr.iit.retrail.server.pip.impl.StandAlonePIP;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.webserver.WebServer;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPSemaphore extends StandAlonePIP implements PIPSemaphoreProtocol {

    protected boolean green = true;
    protected boolean polling = false;
    final public String id = "Semaphore";
    final public String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    final WebServer webServer;
    final URL url;
    final Client client;

    public PIPSemaphore(URL myUrl, boolean green, URL semUrl) throws Exception {
        super();
        log.warn("creating semaphore at URL: {}, initial {} value: {}; namespace: {}; semServer at URL: {}", myUrl, id, green, getClass().getSimpleName(), semUrl);
        url = myUrl;
        client = new Client(semUrl);
        this.log = LoggerFactory.getLogger(PIPSemaphore.class);
        this.green = green;
        this.polling = false;
        this.webServer = Server.createWebServer(myUrl, PIPSemaphoreProtocolProxy.class, getClass().getSimpleName());
    }

    @Override
    public void fireSystemEvent(SystemEvent e) {
        switch (e.type) {
            case beforeApplyChanges:
                PepAttributeInterface a = e.request.getAttribute(category, id);
                if (a != null && polling) {
                    throw new RuntimeException("can't directly set semaphore value because polling is enabled");
                }
                break;
            case afterApplyChanges:
                a = e.request.getAttribute(category, id);
                if(a != null) {
                    String newValueString = e.request.getAttribute(category, id).getValue();
                    log.warn("received new value: {}", newValueString);
                    green = Boolean.parseBoolean(newValueString);
                }
                break;
            default:
                break;
        }
    }

    private boolean getRemoteValue() throws XmlRpcException {
        Object[] params = {};
        //log.warn("invoking SemaphoreServer.getValue()");
        green = (Boolean) client.execute("SemaphoreServer.getValue", params);
        log.warn("green = {}", green);
        return green;
    }

    @Override
    public void refresh(PepRequestInterface request, PepSessionInterface session) {
        if (polling) {
            try {
                green = getRemoteValue();
            } catch (XmlRpcException ex) {
                log.error("while retrieving remote semaphore value: {}", ex);
            }
        }
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#boolean", Boolean.toString(green), "http://localhost:8080/federation-id-prov/saml", category);
        if (polling) {
            test.setExpires(new Date());
        }
        request.replace(test);
    }

    @Override
    public void run() {
    }

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
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

    @Override
    public synchronized boolean setPolling(boolean value) throws Exception {
        log.warn("external semaphore polling now {}", value ? "enabled" : "disabled");
        polling = value;
        return polling;
    }

    @Override
    public synchronized boolean isPolling() {
        return polling;
    }

    @Override
    public synchronized boolean setValue(boolean green) throws Exception {
        if (polling) {
            throw new RuntimeException("can't directly set semaphore value because polling is enabled");
        }
        this.green = green;
        log.warn("setting new value: {}, attributes {}", green);
        //notifyChanges(listManagedAttributes());
        // XXX FIXME Workaround
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#boolean", Boolean.toString(green), "http://localhost:8080/federation-id-prov/saml", category);
        notifyChanges(test);
        return this.green;
    }

    @Override
    public synchronized boolean getValue() throws Exception {
        return polling ? getRemoteValue() : green;
    }

    @Override
    public int setWatchdogPeriod(int newWatchdogPeriod) {
        getUCon().setWatchdogPeriod(newWatchdogPeriod);
        return getUCon().getWatchdogPeriod();
    }

    @Override
    public int getWatchdogPeriod() {
        return getUCon().getWatchdogPeriod();
    }
}
