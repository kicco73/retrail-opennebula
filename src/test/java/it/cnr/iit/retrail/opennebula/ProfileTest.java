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
import java.io.InputStream;
import java.net.URL;
import junit.framework.TestCase;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfileTest extends TestCase {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(ProfileTest.class);
    static final String pepUrlString = "http://0.0.0.0:9081";
    private PEPtest pep = null;
    private PepRequest pepRequest = null;
    private final Object revokeMonitor = new Object();
    private int revoked = 0;
    private PIPSemaphore pipSemaphore;
    private PIPSessions pipSessions;
    
    private class PEPtest extends PEP {

        public PEPtest(URL pdpUrl, URL myUrl) throws Exception {
            super(pdpUrl, myUrl);
        }

        @Override
        public void onObligation(PepSession session, String obligation) throws Exception {
            log.debug("obligation received: {}", obligation);
        }

        @Override
        public void onRevokeAccess(PepSession session) {
            log.debug("revocation received: {}", session);
            synchronized (revokeMonitor) {
                revoked++;
                revokeMonitor.notifyAll();
            }
        }

    }

    public ProfileTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        revoked = 0;
        log.warn("creating ucon server");
        Main.main(null);
        pipSemaphore = (PIPSemaphore) Main.ucon.getPIPChain().get("semaphore");
        pipSessions = (PIPSessions) Main.ucon.getPIPChain().get("sessions");
        log.warn("creating pep client");
        pep = new PEPtest(Main.ucon.myUrl, new URL(pepUrlString));
        pep.setAccessRecoverableByDefault(false);
        InputStream ks = MainTest.class.getResourceAsStream(Main.defaultKeystoreName);
        pep.trustAllPeers(ks, Main.defaultKeystorePassword);
        pep.init();
        //pep.startRecording(new File("retrail-opennebula.xml"));
        pepRequest = PepRequest.newInstance(
                "carniani",
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
    }

    @Override
    protected void tearDown() throws Exception {
        //pep.stopRecording();
        pep.term();
        Main.term();
        super.tearDown();
    }

    private void openConcurrentSessions(int n) throws Exception {
        pipSemaphore.setPolling(false);
        log.info("sequentially opening {} concurrent sessions", n);
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            log.info("opening session:  {} ", i);
            PepSession pepSession = pep.tryAccess(pepRequest);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("ok, {} concurrent sessions opened; total tryAccess time [T{}] = {} ms, normalized {} ms", n, n, elapsedMs, elapsedMs/n);
    }
    
    private void assignConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        pipSemaphore.setPolling(false);
        log.info("sequentially starting {} concurrent sessions");
        long startMs = System.currentTimeMillis();
        int i = 0;
        for (PepSession pepSession: pep.getSessions()) {
            log.info("assigning session: {} ", i++);
            pep.assignCustomId(pepSession.getUuid(), null, "openNebula." + i);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("ok, custom ids assigned; total assignCustomId time [A{}] = {} ms, normalized = {} ms", n, elapsedMs, elapsedMs/n);
    }
    
    private void startConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        pipSemaphore.setPolling(false);
        log.info("sequentially starting concurrent sessions");
        long startMs = System.currentTimeMillis();
        for (PepSession pepSession: pep.getSessions()) {
            log.info("starting session:  {} ", pepSession);
            pepSession = pep.startAccess(pepSession);
            assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("ok, concurrent sessions opened; total startAccess time [St{}] = {} ms, normalized = {} ms",  n, elapsedMs, elapsedMs/n);
    }


    private void setSemaphoreValueViaPIP(boolean value) throws Exception {
        Client pipRpc = new Client(new URL(PIPSemaphore.myUrlString));
        pipRpc.trustAllPeers();
        Object[] params = new Object[]{value};
        log.warn("calling remote PIPSemaphore.setValue({}) at url {}", value, PIPSemaphore.myUrlString);
        pipRpc.execute("PIPSemaphore.setValue", params);
    }

    private void closeConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially closing {} all sessions", n);
        long startMs = System.currentTimeMillis();
        while (!pep.getSessions().isEmpty()) {
            PepSession pepSession = pep.getSessions().iterator().next();
            pep.endAccess(pepSession);
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("all {} sessions closed; total endAccess time [E{}] = {} ms, normalized = {} ms", n, n, elapsedMs, elapsedMs/n);
        assertEquals(0, pipSessions.getSessions());
    }

    private void profileRevocationsViaPIP(int n) throws Exception {
        pipSemaphore.setPolling(false);
        openConcurrentSessions(n);
        assignConcurrentSessions();
        startConcurrentSessions();
        long startMs = System.currentTimeMillis();
        setSemaphoreValueViaPIP(false);
        log.info("waiting for {} revocations", n);
        synchronized (revokeMonitor) {
            while (revoked < n) {
                revokeMonitor.wait();
                log.info("received {} revocations", n);
            }
        }
        long elapsedMs = System.currentTimeMillis() - startMs;
        log.info("{} of {} sessions revoked; total revokeAccess time for PIP [R{}] = {} ms, normalized = {} ms", revoked, n, n, elapsedMs, elapsedMs/n);
        closeConcurrentSessions();
    }

    public void test10_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(10);
        log.info("ok");
    }
    
    public void test20_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(20);
        log.info("ok");
    }
    public void test30_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(30);
        log.info("ok");
    }
    public void test40_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(40);
        log.info("ok");
    }
    public void test50_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(50);
        log.info("ok");
    }
    public void test60_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(60);
        log.info("ok");
    }
    public void test70_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(70);
        log.info("ok");
    }
    public void test80_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(80);
        log.info("ok");
    }
    public void test90_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(90);
        log.info("ok");
    }
    public void testA0_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(100);
        log.info("ok");
    }

    public void testFF_profileRevocationsViaPIP() throws Exception {
        log.info("started");
        Main.ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula_3.xml"));
        profileRevocationsViaPIP(1000);
        log.info("ok");
    }
    
 
}
