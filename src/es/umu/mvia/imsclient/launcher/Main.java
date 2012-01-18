/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.launcher;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import es.umu.mvia.imsclient.IMSClient;
import es.umu.mvia.imsclient.IMSIllegalStateException;
import es.umu.mvia.imsclient.IMSPresenceListener;
import es.umu.mvia.imsclient.service.ims.configuration.AccountInfo;
import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;
/**
 *
 * @author usuario
 */
public class Main {
    private static final String LocalAccountConf = "telvent1@open-ims.test";
	private static final String LocalPassWd = "telvent1";
	private static final List<String> RemoteAccountListConf = Arrays
			.asList(new String[] { "telvent1@open-ims.test", "telvent2@open-ims.test" });

	private static final String SipLogger0 = "IMS Client:";
	private static final String SipLogger1 = SipLogger0+"SIP:"+LocalAccountConf;


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        IMSClient client = new IMSClient();
		Logger.getLogger(SipLogger1).setLevel(Level.FATAL);
		AccountInfo accInfo = null;
		try {
			accInfo = client.getAccountInfo();
		} catch (IMSIllegalStateException e) {
			e.printStackTrace();
		}
		String account = accInfo == null ? null : accInfo.getAccountID();
		System.out.println( "[Presence Server] Programed Account: " + account);
		if (account != null && !account.contains(LocalAccountConf)) {
			try {
				System.out.println( "[Presence Server] Delete Account: " + account);
				client.deleteAccount();
				Logger.getLogger(SipLogger0 + account).setLevel(Level.FATAL);
			} catch (IMSIllegalStateException e) {
				e.printStackTrace();
			}
			account = null;
		}

		if (account == null) {
			System.out.println( "[Presence Server] Create Account: " + LocalAccountConf);
			client.newAccount(LocalAccountConf, LocalPassWd);
			Logger.getLogger(SipLogger1).setLevel(Level.FATAL);
		}

		System.out.println( "[Presence Server] Registring User");
		try {
			client.register();
			Thread.sleep(10000);
		} catch (IMSIllegalStateException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println( "[Presence Server] User Registred is "+client.isRegistered());

    }

}
