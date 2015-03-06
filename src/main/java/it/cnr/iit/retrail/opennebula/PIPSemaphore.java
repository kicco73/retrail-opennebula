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
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import it.cnr.iit.retrail.server.pip.impl.StandAlonePIP;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.webserver.WebServer;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPSemaphore extends StandAlonePIP implements PIPSemaphoreProtocol {
    static final public String category = PepAttribute.CATEGORIES.SUBJECT;
    static public final String myUrlString = "http://0.0.0.0:9082";
    private String attributeId = "Semaphore";
    private boolean value = true;
    private boolean polling = false;
    private final WebServer webServer;
    private final Client client;
    private static PIPSemaphore instance;
    
    static public PIPSemaphore getInstance() {
        return instance;
    }
    
    public PIPSemaphore() throws Exception {
        this.log = LoggerFactory.getLogger(PIPSemaphore.class);
        log.warn("creating semaphore at URL: {}, initial {} value: {}; namespace: {}", myUrlString, attributeId, value, getClass().getSimpleName());
        this.webServer = Server.createWebServer(new URL(myUrlString), PIPSemaphoreProtocolProxy.class, getClass().getSimpleName());
        client = new Client(new URL(SemaphoreServer.myUrlString));
        instance = this;
    }

    @Override
    public void fireSystemEvent(SystemEvent e) {
        switch (e.type) {
            case beforeApplyChanges:
                Collection<PepAttributeInterface> a = e.request.getAttributes(category, attributeId);
                if (!a.isEmpty() && polling) {
                    throw new RuntimeException("can't directly set semaphore value because polling is enabled");
                }
                break;
            case afterApplyChanges:
                a = e.request.getAttributes(category, attributeId);
                if(!a.isEmpty()) {
                    String newValueString = a.iterator().next().getValue();
                    log.warn("received new value: {}", newValueString);
                    value = Boolean.parseBoolean(newValueString);
                }
                break;
            default:
                break;
        }
    }

    private boolean getRemoteValue() throws XmlRpcException {
        Object[] params = {};
        log.warn("invoking SemaphoreServer.getValue()");
        value = (Boolean) client.execute("SemaphoreServer.getValue", params);
        log.warn("green = {}", value);
        return value;
    }

    @Override
    public void refresh(PepRequestInterface request, PepSessionInterface session) {
        //log.warn("refreshing semaphore value");
        if (polling) {
            try {
                value = getRemoteValue();
            } catch (XmlRpcException ex) {
                log.error("while retrieving remote semaphore value: {}", ex);
            }
        }
        PepAttributeInterface test = newSharedAttribute(attributeId, PepAttribute.DATATYPES.BOOLEAN, value, category);
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
        this.value = green;
        log.warn("setting new value: {}, attributes {}", green);
        //notifyChanges(listManagedAttributes());
        // XXX FIXME Workaround
        PepAttributeInterface test = newSharedAttribute(getAttributeId(), PepAttribute.DATATYPES.BOOLEAN, green, category);
        notifyChanges(test);
        return this.value;
    }
    
    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }
    
    @Override
    public synchronized boolean getValue() throws Exception {
        return polling ? getRemoteValue() : value;
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
