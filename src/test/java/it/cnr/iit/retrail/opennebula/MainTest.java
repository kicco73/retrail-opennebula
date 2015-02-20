/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainTest extends TestCase {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(MainTest.class);
    static final String pepUrlString = "http://0.0.0.0:8081";
    private boolean revoked = false;
    private PEPtest pep = null;
    private PepRequest pepRequest = null;
    private final Object revokeMonitor = new Object();
    
    private class PEPtest extends PEP {

        public PEPtest(URL pdpUrl, URL myUrl) throws Exception {
            super(pdpUrl, myUrl);
        }
        
        @Override
        public void onObligation(PepSession session, String obligation) throws Exception {
            log.warn("obligation received: {}", obligation);
        }
        
        @Override
        public void onRevokeAccess(PepSession session) {
            log.warn("revocation received: {}", session);
            synchronized(revokeMonitor) {
                revoked = true;
                revokeMonitor.notifyAll();
            }
        }
        
        public void setSemaphoreValue(boolean value) throws Exception {
            Client pipRpc = new Client(Main.pipSemaphore.url);
            pipRpc.trustAllPeers();
            Object []params = new Object[]{value};
            log.warn("calling remote PIPSemaphore.setValue({}) at url {}", value, Main.pipSemaphore.url);
            pipRpc.startRecording(new File("pipsemaphore.xml"));
            pipRpc.execute("PIPSemaphore.setValue", params);
            pipRpc.stopRecording();
        }
        
        public void setSemaphoreValueViaPepInterface(PepSession session, boolean value) throws Exception {
            PepRequest req = new PepRequest();
            PepAttribute attribute = new PepAttribute(
                Main.pipSemaphore.id,
                PepAttribute.DATATYPES.BOOLEAN,
                Boolean.toString(value),
                "issuer",
                Main.pipSemaphore.category);
            req.add(attribute);
            log.warn("calling remote pep.applyChanges({}, {})", req, value);
            pep.applyChanges(session, req);
        }
        
        public void setExternalSemaphoreValue(boolean value) throws Exception {
            Client pipRpc = new Client(Main.semaphoreServer.myUrl);
            pipRpc.trustAllPeers();
            Object []params = new Object[]{value};
            log.warn("calling remote SemaphoreServer.setValue({}) at url {}", value, Main.semaphoreServer.myUrl);
            pipRpc.startRecording(new File("semaphoreserver.xml"));
            pipRpc.execute("SemaphoreServer.setValue", params);
            pipRpc.stopRecording();
        }
    }
    
    public MainTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        revoked = false;
        log.warn("creating ucon server");
        Main.main(null);
        log.warn("creating pep client");
        pep = new PEPtest(new URL(Main.pdpUrlString), new URL(pepUrlString));
        pep.setAccessRecoverableByDefault(false);
        pep.init();
        pep.startRecording(new File("retrail-opennebula.xml"));
        pepRequest = PepRequest.newInstance(
            "carniani",
            "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
            " ",
            "issuer");
    }
    
    @Override
    protected void tearDown() throws Exception {
        pep.stopRecording();
        pep.term();
        Main.term();
        super.tearDown();
    }

    /**
     * Test of main method, of class Main.
     * @throws java.lang.Exception
     */
    public void testLocalToggle() throws Exception {
        log.warn("testing local semaphore toggle");
        Main.pipSemaphore.setPolling(false);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testLocalToggle");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        Main.pipSemaphore.setValue(false);
        Thread.sleep(1000);
        assertEquals(true, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, Main.pipSessions.getSessions());
    }

    public void testRemoteToggle() throws Exception {
        log.warn("testing remote semaphore toggle");
        Main.pipSemaphore.setPolling(false);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggle");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(false, revoked);
        pep.setSemaphoreValue(false);
        Thread.sleep(1000);
        assertEquals(true, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, Main.pipSessions.getSessions());
    }
    
        public void testRemoteToggleViaPepInterface() throws Exception {
        log.warn("testing remote semaphore toggle through the basic Pep Interface");
        Main.pipSemaphore.setPolling(false);
        Main.ucon.setWatchdogPeriod(0);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggleViaPepInterface");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(false, revoked);
        pep.setSemaphoreValueViaPepInterface(pepSession, false);
        synchronized(revokeMonitor) {
            if(!revoked)
                revokeMonitor.wait();
        }
        assertEquals(true, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, Main.pipSessions.getSessions());
    }
    
    public void testConcurrentTryAccess() throws Exception {
        Main.pipSemaphore.setPolling(false);
        List<PepSession> sessions = new ArrayList<>(11);
        for(int i = 0; i < 3; i++) {
            log.warn("testing concurrent try access {}", i);
            assertEquals(i, Main.pipSessions.getSessions());
            PepSession pepSession = pep.tryAccess(pepRequest);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
            pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggle."+i);
            assertEquals(i, Main.pipSessions.getSessions());
            pepSession = pep.startAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
            sessions.add(pepSession);
        }
        log.info("ok, 3 concurrent tries admitted");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pepSession = pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        for(int i = 2; i >= 0; i--) {
            log.warn("ending session {}", i);
            pepSession = sessions.get(i);
            assertEquals(i+1, Main.pipSessions.getSessions());
            pepSession = pep.endAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        assertEquals(0, Main.pipSessions.getSessions());
    }
    
    public void testRemoteToggleWithExternalSemaphore() throws Exception {
        log.warn("testing remote toggle with external semaphore");
        Main.pipSemaphore.setPolling(true);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggleWithExternalSemaphore");
        pep.setExternalSemaphoreValue(false);
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        pep.endAccess(pepSession);
        assertEquals(0, Main.pipSessions.getSessions());
    }

    public void testRemoteRevocationWithExternalSemaphore() throws Exception {
        log.warn("testing remote revocation with external semaphore");
        Main.pipSemaphore.setPolling(true);
        Main.ucon.setWatchdogPeriod(1);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteRevocationWithExternalSemaphore");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(false, revoked);
        pep.setExternalSemaphoreValue(false);
        Thread.sleep(2000);
        assertEquals(true, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, Main.pipSessions.getSessions());
    }
}
