/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient;

import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;

/**
 *
 * @author Usuario
 */
public interface IMSPresenceListener {

    public void contactStateChanged(String contactID, PresenceInfo presenceInfo);

}
