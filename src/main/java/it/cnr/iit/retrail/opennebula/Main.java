/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.impl.UConFactory;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class Main {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    static public final String pdpUrlString = "http://0.0.0.0:9080";
    static public final String pipUrlString = "http://0.0.0.0:9082";
    static public final String semUrlString = "http://0.0.0.0:9083";
    static public UCon ucon = null;
    static public PIPSemaphore pipSemaphore = null;
    static public PIPSessions pipSessions = null;
    static public SemaphoreServer semaphoreServer = null;
    
    static public void main(String[] argv) throws Exception {
            log.info("Setting up Semaphore server...");
            semaphoreServer = new SemaphoreServer(new URL(semUrlString), true);
            semaphoreServer.init();

            log.info("Setting up Ucon server...");
            ucon = (UCon) UConFactory.getInstance(new URL(pdpUrlString));
            if(argv != null && argv.length > 0) {
                File f = new File(argv[0]);
                log.warn("using file-system behaviour file located at: {}", f.getAbsolutePath());
                ucon.loadConfiguration(new FileInputStream(f));
            } else {
                log.warn("using internally packaged policy set");
                ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula.xml"));
            }
            ucon.maxMissedHeartbeats = 3600;
            ucon.setWatchdogPeriod(0);
            pipSemaphore = new PIPSemaphore(new URL(pipUrlString), true, new URL(semUrlString));
            ucon.getPIPChain().add(pipSemaphore);
            pipSessions = new PIPSessions();
            ucon.getPIPChain().add(pipSessions);
            ucon.init();            
        }

    static public void term() throws InterruptedException {
        ucon.term();
        semaphoreServer.term();
    }
}
