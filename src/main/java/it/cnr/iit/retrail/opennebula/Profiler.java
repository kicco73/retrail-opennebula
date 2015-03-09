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
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */

public class Profiler {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(Profiler.class);
    String pepUrlString = "http://146.48.99.95:9081";
    String pdpUrlString = "http://146.48.99.87:9080";
    String pipUrlString = "http://146.48.99.87:9082";
    private PEPtest pep = null;
    private PepRequest pepRequest = null;
    static private final Object revokeMonitor = new Object();
    static private int revoked = 0;
    private double T = 0.0;
    private double A = 0.0;
    private double S = 0.0;
    private double R = 0.0;
    private double E = 0.0;
    private int c = 0;
    
    private class PEPtest extends PEP {

        public PEPtest(URL pdpUrl, URL myUrl) throws Exception {
            super(pdpUrl, myUrl);
        }

        @Override
        public void onRecoverAccess(PepSession session) throws Exception {
            // Remove previous run stale sessions
            log.warn("Ending stale session {}", session);
            endAccess(session);
        }

        @Override
        public void onObligation(PepSession session, String obligation) throws Exception {
            log.debug("obligation received: {}", obligation);
        }

        @Override
        public void onRevokeAccess(PepSession session) {
            //log.warn("* revocation received: {}", session);
            synchronized (revokeMonitor) {
                revoked++;
                //log.warn("* revocation notifying: {}", session);
                revokeMonitor.notifyAll();
            }
        }
    }

    public static void main(String argv[]) throws Exception {
        Profiler p = new Profiler();
        p.pepUrlString = argv[1];
        p.pdpUrlString = argv[2];
        p.pipUrlString = argv[3];
        p.run(Integer.parseInt(argv[0]), "output.csv");
        
    }
    
    private void run(int repeats, String csvName) throws Exception  {
        log.warn("creating pep client");
        pep = new PEPtest(new URL(pdpUrlString), new URL(pepUrlString));
        InputStream ks = Main.class.getResourceAsStream(Main.defaultKeystoreName);
        pep.trustAllPeers(ks, Main.defaultKeystorePassword);
        pep.init();
        pepRequest = PepRequest.newInstance(
                "carniani",
                "urn:fedora:names:fedora:2.1:action:id-getDatastreamDissemination",
                " ",
                "issuer");
        new File(csvName).delete();
        try (FileWriter o = new FileWriter(csvName)) {
            o.write("requests,sampleNo,T,A,S,R,E\n");
            for(int exp = 0; exp < 12; exp++) {
                for(int cycle = 1; cycle <= repeats; cycle++) {
                    log.info("starting {} calls profiling test (cycle {} of {})", 1<<exp, cycle, repeats);
                    profileRevocationsViaPIP(1<<exp);
                    o.write((1<<exp)+","+
                            cycle+","+
                            T+","+
                            A+","+
                            S+","+
                            R+","+
                            E+"\n");
                    log.info("ok");        
                }
            }
        } catch(Exception e) {
            log.error("while profiling: {}", e);
            throw e;
        }
        pep.term();
    }

    private void openConcurrentSessions(int n) throws Exception {
        log.info("sequentially opening {} concurrent sessions", n);
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            PepSession pepSession = pep.tryAccess(pepRequest);
            //log.info("opening session: {} {}", i+1, pepSession);
        }
        T = (System.currentTimeMillis() - startMs)/1000.0;
        log.info("ok, {} concurrent sessions opened; total tryAccess time [T{}] = {} s, normalized {} s", n, n, T, T / n);
    }

    private void assignConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially assigning {} concurrent sessions", n);
        long startMs = System.currentTimeMillis();
        int i = 1;
        for (PepSession pepSession : pep.getSessions()) {
            //log.info("assigning session: {} {}", i+1, pepSession);
            pep.assignCustomId(pepSession.getUuid(), null, "openNebula." + c);
            i++;
            c++;
        }
        A = (System.currentTimeMillis() - startMs) / 1000.0;
        log.info("ok, custom ids assigned; total assignCustomId time [A{}] = {} s, normalized = {} s", n, A, A / n);
    }

    private void startConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially starting {} concurrent sessions", n);
        long startMs = System.currentTimeMillis();
        int i = 0;
        for (PepSession pepSession : pep.getSessions()) {
            //log.info("starting session: {} {} ", ++i, pepSession);
            pepSession = pep.startAccess(pepSession);
        }
        S = (System.currentTimeMillis() - startMs)/1000.0;
        log.info("ok, {} concurrent sessions started; total startAccess time [St{}] = {} s, normalized = {} s", n, n, S, S / n);
    }

    private void setSemaphoreValueViaPIP(boolean value) throws Exception {
        Client pipRpc = new Client(new URL(pipUrlString));
        pipRpc.trustAllPeers();
        Object[] params = new Object[]{value};
        log.warn("calling remote PIPSemaphore.setValue({}) at url {}", value, pipUrlString);
        pipRpc.execute("PIPSemaphore.setValue", params);
    }

    private void closeConcurrentSessions() throws Exception {
        int n = pep.getSessions().size();
        log.info("sequentially closing {} sessions", n);
        long startMs = System.currentTimeMillis();
        while (!pep.getSessions().isEmpty()) {
            PepSession pepSession = pep.getSessions().iterator().next();
            pep.endAccess(pepSession);
            assert(PepResponse.DecisionEnum.Permit == pepSession.getDecision());
        }
        E = (System.currentTimeMillis() - startMs)/1000.0;
        log.info("all {} sessions closed; total endAccess time [E{}] = {} ms, normalized = {} ms", n, n, E, E / n);
    }

    private void profileRevocationsViaPIP(int n) throws Exception {
        setSemaphoreValueViaPIP(true);
        revoked = 0;
        openConcurrentSessions(n);
        assignConcurrentSessions();
        startConcurrentSessions();
        long startMs = System.currentTimeMillis();
        setSemaphoreValueViaPIP(false);
        log.info("waiting for {} revocations", n);
        synchronized (revokeMonitor) {
            long start = System.currentTimeMillis();
            while (revoked < n) {
                revokeMonitor.wait(3000);
                if(System.currentTimeMillis()-start >=3000)
                    throw new TimeoutException("while waiting for revocation; expected "+n+", currently received "+revoked);
                log.info("received {} revocations", revoked);
            }
        }
        R = (System.currentTimeMillis() - startMs)/1000.0;
        log.info("{} of {} sessions revoked; total revokeAccess time for PIP [R{}] = {} s, normalized = {} s", revoked, n, n, R, R / n);
        closeConcurrentSessions();
    }

}
