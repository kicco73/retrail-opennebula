/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.opennebula;

import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UCon;
import java.net.URL;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class Main {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);
    static public final String pdpUrlString = "http://0.0.0.0:8081";
    static private UConInterface ucon = null;
    
    static private void changePoliciesTo(String prePath, String onPath, String postPath, String tryStartPath, String tryEndPath) {
        ucon.setPolicy(UConInterface.PolicyEnum.PRE, Main.class.getResource(prePath));
        ucon.setPolicy(UConInterface.PolicyEnum.ON, Main.class.getResource(onPath));
        ucon.setPolicy(UConInterface.PolicyEnum.POST, Main.class.getResource(postPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYSTART, Main.class.getResource(tryStartPath));
        ucon.setPolicy(UConInterface.PolicyEnum.TRYEND, Main.class.getResource(tryEndPath));
    }
    
    static public void main(String[] argv) throws Exception {
            log.info("Setting up Ucon embedded server...");
            ucon = UCon.getInstance(new URL(pdpUrlString));
            changePoliciesTo("/META-INF/policies/pre.xml",
                             "/META-INF/policies/on.xml",
                             "/META-INF/policies/post.xml",
                             "/META-INF/policies/trystart.xml",
                             "/META-INF/policies/tryend.xml"
            );
            ucon.init();            
        }

}
