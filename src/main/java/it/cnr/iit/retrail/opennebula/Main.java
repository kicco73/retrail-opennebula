/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import it.cnr.iit.retrail.server.pip.impl.PIPSessions;
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
    static public UCon ucon = null;
    static public PIPSemaphore pipSemaphore = null;
    static public PIPSessions pipSessions = null;
    
    static private void changePoliciesTo(String prePath, String onPath, String postPath, String tryStartPath, String tryEndPath) throws Exception {
        ucon.setPolicy(UConInterface.PolicyEnum.PRE, Main.class.getResource(prePath));
        ucon.setPolicy(UConInterface.PolicyEnum.ON, Main.class.getResource(onPath));
        ucon.setPolicy(UConInterface.PolicyEnum.POST, Main.class.getResource(postPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYSTART, Main.class.getResource(tryStartPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYEND, Main.class.getResource(tryEndPath));
    }
    
    static public void main(String[] argv) throws Exception {
            log.info("Setting up Ucon server...");
            ucon = (UCon) UCon.getInstance(new URL(pdpUrlString));
            changePoliciesTo("/META-INF/policies/opennebula-pre.xml",
                             "/META-INF/policies/opennebula-on.xml",
                             "/META-INF/policies/opennebula-post.xml",
                             "/META-INF/policies/opennebula-trystart.xml",
                             "/META-INF/policies/opennebula-tryend.xml"
            );
            ucon.maxMissedHeartbeats = 100;
            ucon.watchdogPeriod = 10000;
            pipSemaphore = new PIPSemaphore(new URL(pipUrlString), true);
            ucon.addPIP(pipSemaphore);
            pipSessions = new PIPSessions();
            ucon.addPIP(pipSessions);
            ucon.init();            
        }

}
