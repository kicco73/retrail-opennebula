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
import java.io.InputStream;
import java.net.URL;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class Main {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

    static final String defaultKeystoreName = "/META-INF/keystore.jks";
    static final String defaultKeystorePassword = "uconas4wc";
    static public UCon ucon = null;
    static public SemaphoreServer semaphoreServer = null;
    
    static public void main(String[] argv) throws Exception {
            log.info("Setting up Semaphore server...");
            semaphoreServer = new SemaphoreServer();
            semaphoreServer.init();

            String pdpUrlString = argv != null && argv.length > 0? 
                    argv[0] : "https://0.0.0.0:9080";
            URL pdpUrl = new URL(pdpUrlString);
            log.info("Setting up Ucon server at {}...", pdpUrlString);
            ucon = UConFactory.getInstance(pdpUrl);
            // Telling server to use a self-signed certificate and
            // trust any client.
            InputStream ks = Main.class.getResourceAsStream(defaultKeystoreName);
            ucon.trustAllPeers(ks, defaultKeystorePassword);
            
            ucon.setMaxMissedHeartbeats(3600);
            ucon.setWatchdogPeriod(0);

            if(argv != null && argv.length > 1) {
                File f = new File(argv[1]);
                log.info("using file-system behaviour file located at: {}", f.getAbsolutePath());
                ucon.loadConfiguration(new FileInputStream(f));
            } else {
                log.warn("using builtin behaviour");
                ucon.loadConfiguration(Main.class.getResourceAsStream("/ucon-opennebula.xml"));
            }

            // start server
            ucon.init();
        }

    static public void term() throws InterruptedException {
        ucon.term();
        semaphoreServer.term();
    }
}
