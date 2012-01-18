/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.service.ims.presence;

import es.umu.mvia.imsclient.service.protocol.Contact;
import java.util.Map;



/**
 *
 * @author Usuario
 */
public interface IMSPresenceService {

        public void addContact(String id, String group);

        public void removeContact(String id);
        
        public void removeAllContacts();

        public Contact findContactByID(String contact);

        public Map<String,PresenceInfo> getContactsInfo();

        public void changeState(String newSate);

        public void changeState(String newSate, String note);

        public void unregister();

        public void register();

        public String getPresenceState();

}
