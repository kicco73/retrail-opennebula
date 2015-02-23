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
import java.net.URL;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class Main {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    static public final String pdpUrlString = "http://0.0.0.0:8080";
    static public final String pipUrlString = "http://0.0.0.0:8082";
    static public final String semUrlString = "http://0.0.0.0:8083";
    static public UCon ucon = null;
    static public PIPSemaphore pipSemaphore = null;
    static public PIPSessions pipSessions = null;
    static public SemaphoreServer semaphoreServer = null;
    
    static private void changePoliciesTo(String prePath, String onPath, String postPath, String tryStartPath, String tryEndPath) throws Exception {
        log.warn("using internally packaged policy set");
        ucon.setPolicy("init-tryaccess", Main.class.getResourceAsStream(prePath));
        ucon.setPolicy("ongoing-ongoingaccess", Main.class.getResourceAsStream(onPath));
        ucon.setPolicy("ongoing-endaccess", Main.class.getResourceAsStream(postPath));
        ucon.setPolicy("try-startaccess", Main.class.getResourceAsStream(tryStartPath));
        ucon.setPolicy("try-endaccess", Main.class.getResourceAsStream(tryEndPath));
    }
    
    static private void readPoliciesFromDir(String dirPath) throws Exception {
        File f = new File(dirPath);
        dirPath = "file:"+f.getAbsolutePath();
        log.warn("using file-system policy set located at dir: {}", f.getAbsolutePath());
        ucon.setPolicy("init-tryaccess", new URL(dirPath+"/opennebula-pre.xml"));
        ucon.setPolicy("ongoing-ongoingaccess", new URL(dirPath+"/opennebula-on.xml"));
        ucon.setPolicy("ongoing-endaccess", new URL(dirPath+"/opennebula-post.xml"));
        ucon.setPolicy("try-startaccess", new URL(dirPath+"/opennebula-trystart.xml"));
        ucon.setPolicy("try-endaccess", new URL(dirPath+"/opennebula-tryend.xml"));
    }
    
    static public void main(String[] argv) throws Exception {
            log.info("Setting up Semaphore server...");
            semaphoreServer = new SemaphoreServer(new URL(semUrlString), true);
            semaphoreServer.init();

            log.info("Setting up Ucon server...");
            ucon = (UCon) UConFactory.getInstance(new URL(pdpUrlString));
            if(argv != null && argv.length > 0)
                readPoliciesFromDir(argv[0]);
            else
                changePoliciesTo("/META-INF/policies/opennebula-pre.xml",
                                 "/META-INF/policies/opennebula-on.xml",
                                 "/META-INF/policies/opennebula-post.xml",
                                 "/META-INF/policies/opennebula-trystart.xml",
                                 "/META-INF/policies/opennebula-tryend.xml"
                );
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
