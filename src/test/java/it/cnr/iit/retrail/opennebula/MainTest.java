/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import java.io.File;
import java.io.InputStream;
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
    static final String pepUrlString = "https://0.0.0.0:9081";
    private int revoked = 0;
    private PEPtest pep = null;
    private PepRequest pepRequest = null;
    private final Object revokeMonitor = new Object();
    private PIPSemaphore pipSemaphore;
    private PIPSessions pipSessions;

    private class PEPtest extends PEP {

        public PEPtest(URL pdpUrl, URL myUrl) throws Exception {
            super(pdpUrl, myUrl);
        }

        @Override
        public void onRecoverAccess(PepSession session) throws Exception {
            // Remove previous run stale sessions
            endAccess(session);
        }

        @Override
        public void onObligation(PepSession session, String obligation) throws Exception {
            log.warn("obligation received: {}", obligation);
        }

        @Override
        public void onRevokeAccess(PepSession session) {
            log.warn("revocation received: {}", session);
            synchronized (revokeMonitor) {
                revoked++;
                revokeMonitor.notifyAll();
            }
        }

    }

    public MainTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        revoked = 0;
        log.warn("creating ucon server");
        Main.main(null);
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));

        pipSemaphore = (PIPSemaphore) Main.ucon.getPIPChain().get("semaphore");
        pipSessions = (PIPSessions) Main.ucon.getPIPChain().get("sessions");
        log.warn("creating pep client");
        pep = new PEPtest(Main.ucon.myUrl, new URL(pepUrlString));
        pep.trustAllPeers();
        // Allowing client to accept a self-signed certificate;
        // allow callbacks to the pep for untrusted ucons.
        InputStream ks = MainTest.class.getResourceAsStream(Main.defaultKeystoreName);
        pep.trustAllPeers(ks, Main.defaultKeystorePassword);
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

    private void setSemaphoreValue(boolean value) throws Exception {
        Client pipRpc = new Client(new URL(PIPSemaphore.myUrlString));
        pipRpc.trustAllPeers();
        Object[] params = new Object[]{value};
        log.warn("calling remote PIPSemaphore.setValue({}) at url {}", value, PIPSemaphore.myUrlString);
        pipRpc.startRecording(new File("pipsemaphore.xml"));
        pipRpc.execute("PIPSemaphore.setValue", params);
        pipRpc.stopRecording();
    }

    private void setExternalSemaphoreValue(boolean value) throws Exception {
        Client pipRpc = new Client(Main.semaphoreServer.myUrl);
        pipRpc.trustAllPeers();
        Object[] params = new Object[]{value};
        log.warn("calling remote SemaphoreServer.setValue({}) at url {}", value, Main.semaphoreServer.myUrl);
        pipRpc.startRecording(new File("semaphoreserver.xml"));
        pipRpc.execute("SemaphoreServer.setValue", params);
        pipRpc.stopRecording();
    }

    /**
     * Test of main method, of class Main.
     *
     * @throws java.lang.Exception
     */

    public void testDummy() {
    }
    /*
    public void testLocalToggle() throws Exception {
        log.warn("testing local semaphore toggle");
        pipSemaphore.setPolling(false);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testLocalToggle");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pipSemaphore.setValue(false);
        Thread.sleep(1000);
        assertEquals(1, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
    }

    public void testRemoteToggle() throws Exception {
        log.warn("testing remote semaphore toggle");
        pipSemaphore.setPolling(false);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggle");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(0, revoked);
        setSemaphoreValue(false);
        Thread.sleep(1000);
        assertEquals(1, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
    }

    public void testConcurrentTryAccess() throws Exception {
        pipSemaphore.setPolling(false);
        List<PepSession> sessions = new ArrayList<>(11);
        for (int i = 0; i < 3; i++) {
            log.warn("testing concurrent try access {}", i);
            assertEquals(i, pipSessions.getSessions());
            PepSession pepSession = pep.tryAccess(pepRequest);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
            pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggle." + i);
            assertEquals(i, pipSessions.getSessions());
            pepSession = pep.startAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
            sessions.add(pepSession);
        }
        log.info("ok, 3 concurrent tries admitted");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pepSession = pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        for (int i = 2; i >= 0; i--) {
            log.warn("ending session {}", i);
            pepSession = sessions.get(i);
            assertEquals(i + 1, pipSessions.getSessions());
            pepSession = pep.endAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        assertEquals(0, pipSessions.getSessions());
    }

    public void testRemoteToggleWithExternalSemaphore() throws Exception {
        log.warn("testing remote toggle with external semaphore");
        pipSemaphore.setPolling(true);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteToggleWithExternalSemaphore");
        setExternalSemaphoreValue(false);
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Deny, pepSession.getDecision());
        pep.endAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
    }

    public void testRemoteRevocationWithExternalSemaphore() throws Exception {
        log.warn("testing remote revocation with external semaphore");
        pipSemaphore.setPolling(true);
        Main.ucon.setWatchdogPeriod(1);
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.assignCustomId(pepSession.getUuid(), null, "testRemoteRevocationWithExternalSemaphore");
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        assertEquals(0, revoked);
        log.warn("waiting for revocation...");
        setExternalSemaphoreValue(false);
        Thread.sleep(2000);
        assertEquals(1, revoked);
        pep.endAccess(pepSession);
        assertEquals(0, pipSessions.getSessions());
    }
    */
}
