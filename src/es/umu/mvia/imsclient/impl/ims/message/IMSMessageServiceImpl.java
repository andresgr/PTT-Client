/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.impl.ims.message;

import es.umu.mvia.imsclient.service.ims.message.IMSMessageService;
import es.umu.mvia.imsclient.service.ims.presence.IMSPresenceService;
import es.umu.mvia.imsclient.service.protocol.*;
import es.umu.mvia.imsclient.util.Logger;

/**
 *
 * @author Usuario
 */
public class IMSMessageServiceImpl implements IMSMessageService {

    private Logger logger = Logger.getLogger(IMSMessageServiceImpl.class);

    private ProtocolProviderService protocolProviderService = null;

    private IMSPresenceService presenceService = null;

    public IMSMessageServiceImpl(ProtocolProviderService pps,IMSPresenceService imsps){
        protocolProviderService = pps;
        presenceService = imsps;
    }


            /*IMS Message Service Implementation*/
    /**
     *
     * @param contact
     * @param text
     */
    public void sendMessage(String contact, String text) {
        
        if (text == null) {
            logger.error("[MessageServiceImpl]Sending empty messages is not supported");
            return;
        }

        if (contact == null) {
            logger.error("[MessageServiceImpl]Contact not valid");
            return;
        }
        
        if (!assertOnline()) {
            logger.error("El servicio de presencia no se encuentra activo");
            return;
        }

        logger.debug("Enviando: " + text + " a " + contact);
        //Comprobar que el contacto esta en la lista del usuario
        Contact dest = null;
        try {
            if ((dest = presenceService.findContactByID(contact)) == null) {
                presenceService.addContact(contact,"NotInContactList");
                dest = presenceService.findContactByID(contact);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
        
        OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging) protocolProviderService
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        Message msg = im.createMessage(text);

        try {
            im.sendInstantMessage(dest, msg);
        } catch (IllegalStateException ex) {
            logger.error("Failed to send message:\n" + ex);

        }catch (Exception ex){
            logger.error("Failed to send message:\n" + ex);
        }
    }

                /* Private methods */
    /**
     *
     * @return
     */
    private boolean assertOnline(){
        if (protocolProviderService == null || presenceService == null)
            return false;

        return true;
    }
}
