/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.client.impl.PEP;
import it.cnr.iit.retrail.commons.Client;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.io.File;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import junit.framework.TestCase;
import org.apache.xmlrpc.XmlRpcException;
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
    
    private class PEPtest extends PEP {

        public PEPtest(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException, NoSuchAlgorithmException, KeyManagementException {
            super(pdpUrl, myUrl);
        }
        
        @Override
        public void onObligation(PepSession session, String obligation) throws Exception {
            log.warn("obligation received: {}", obligation);
        }
        
        @Override
        public void onRevokeAccess(PepSession session) {
            log.warn("revocation received: {}", session);
            revoked = true;
        }
        public void setSemaphoreValue(boolean value) throws Exception {
            Client pipRpc = new Client(Main.pipSemaphore.url);
            Object []params = new Object[]{value};
            log.warn("calling remote PIPSemaphore.setValue({}) at url {}", value, Main.pipSemaphore.url);
            pipRpc.startRecording(new File("pipsemaphore.xml"));
            pipRpc.execute("PIPSemaphore.setValue", params);
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
        pepRequest = PepRequest.newInstance(
            "carniani",
            "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
            " ",
            "issuer");
    }
    
    @Override
    protected void tearDown() throws Exception {
        pep.term();
        Main.ucon.term();
        super.tearDown();
    }

    /**
     * Test of main method, of class Main.
     * @throws java.lang.Exception
     */
    public void testLocalToggle() throws Exception {
        log.warn("testing local semaphore toggle");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        Main.pipSemaphore.setValue(false);
        Thread.sleep(1000);
        assertEquals(true, revoked);
    }

    public void testRemoteToggle() throws Exception {
        log.warn("testing remote semaphore toggle");
        PepSession pepSession = pep.tryAccess(pepRequest);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.startAccess(pepSession);
        assertEquals(PepResponse.DecisionEnum.Permit, pepSession.getDecision());
        pep.setSemaphoreValue(false);
        Thread.sleep(1000);
        assertEquals(true, revoked);
    }
}
