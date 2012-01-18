/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.impl.ims.presence;

import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;
import es.umu.mvia.imsclient.impl.protocol.sip.SipStatusEnum;
import es.umu.mvia.imsclient.service.protocol.*;
import es.umu.mvia.imsclient.service.ims.presence.IMSPresenceService;
import es.umu.mvia.imsclient.util.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 * @author Usuario
 */
public class IMSPresenceServiceImpl implements IMSPresenceService {

    private final String DEFAULT_XCAP_FILE = "index";
    private final String DEFAULT_GROUP = "friends";
    private final long FINAL_PUBLISH_PERIOD = 2000;

    private Logger logger = Logger.getLogger(IMSPresenceServiceImpl.class);

    private ProtocolProviderFactory protocolProviderFactory = null;

    private ProtocolProviderService protocolProviderService = null;

    //private MetaContactListService metaContactListService = null;

    //private ConfigurationService configurationService = null;

    //private FileAccessService faService = null;

    private XCAPClient xcapClient;

    public IMSPresenceServiceImpl(ProtocolProviderService pps, ProtocolProviderFactory ppf) {

        protocolProviderFactory = ppf;
        protocolProviderService = pps;

        //configurationService = new ConfigurationServiceImpl();
        //faService = new FileAccessServiceImpl();

        /*metaContactListService = new MetaContactListServiceImpl(protocolProviderFactory,
                configurationService, faService);*/
        try {
            initializeXCAPClient(pps.getAccountID().getUserID());
            //xcapClient.deleteDocument(pps.getAccountID().getUserID(),"index");
            /*if (xcapClient != null) {
            xcapClient.createInitialDocument(pps.getAccountID().getUserID(), "index");
            }
            xcapClient.createContact("andres@open-ims.test", "friends");*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //xcapClient.deleteDocument(pps.getAccountID().getUserID(),"index");

        /*if (xcapClient != null) {
            xcapClient.createInitialDocument(pps.getAccountID().getUserID(), "index");
        }

        xcapClient.createContact("andres@open-ims.test", "friends");*/
    }


    private void initializeXCAPClient(String userId) throws Exception {
        xcapClient = new XCAPClient(
                (String)protocolProviderService.getAccountID().getAccountProperties().
                    get(ProtocolProviderFactory.PROXY_ADDRESS),
                    8888, "/mobicents", userId);

        if (xcapClient != null) {
            xcapClient.createInitialDocument(DEFAULT_XCAP_FILE);
        }
    }


            /* Implementation of the Presence Service methods*/

    /**
     *
     * @param contact
     * @return
     */
    public Contact findContactByID(String contact) {
        if (! assertRegistered()) {
            register();
        }
        OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                getOperationSet(OperationSetPresence.class);
        if (presence != null) {
            return presence.findContactByID(contact);
        }
        logger.error("No se pudo recuperar la referencia a servicio de presencia");
        return null;
    }


    /**
     *
     * @param id
     * @return
     */
    public void addContact(String id, String group) {
        /*if (! assertRegistered()) {
            logger.w("Es necesario estar registrado en el core para añadir un contacto");
            return;
        }*/

        if (group == null)
            group = DEFAULT_GROUP;

        if (xcapClient != null) {
            xcapClient.createContact(DEFAULT_XCAP_FILE, id, group);
        }


        /*MetaContactGroup rootGroup = metaContactListService.getRoot();
        if (rootGroup.getMetaContactSubgroup(group) == null) {
            logger.debug("Creando el grupo " + group);
            metaContactListService.createMetaContactGroup(metaContactListService.getRoot(),group);
        }
        Contact contact = null;
        contact = this.findContactByID(id);

        if (contact != null){
            boolean inNotInContactList = false;
            Map<String,PresenceInfo> m = this.getContactsInfo();
            if (m != null) {
                Iterator<String> it = m.keySet().iterator();
                while (it.hasNext()) {
                       String idContact = it.next();
                       PresenceInfo pi = m.get(id);
                       if (idContact.equalsIgnoreCase(id) &&
                               pi.getGroupName().equalsIgnoreCase("NotInContactList")) 
                             metaContactListService.removeContact(contact);

                        metaContactListService.createMetaContact(protocolProviderService,
                                   rootGroup.getMetaContactSubgroup(group), id);
                        inNotInContactList = true;
                        break;
                }
            }
            if (!inNotInContactList )
                logger.debug(" ID de usuario duplicado, no se inserta el contacto.");

        }else{
                metaContactListService.createMetaContact(protocolProviderService,
                rootGroup.getMetaContactSubgroup(group), id);
        }*/
    }


    /**
     *
     * @param id
     */
    public void removeContact(String id) {

        if (xcapClient != null) {
            xcapClient.deleteContact(DEFAULT_XCAP_FILE, id, "friends");
        }

        //Si estamos conectados al core
/*        if (protocolProviderService != null) {
            Contact contact = null;
            logger.debug("Eliminando el contacto " + id);
            OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                    getOperationSet(OperationSetPresence.class);
            if (presence != null) {
                contact = presence.findContactByID(id);

                //Si encontramos el contacto
                if (contact != null) {
                    metaContactListService.removeContact(contact);

                    ContactGroup group = contact.getParentContactGroup();
                    if (group.countContacts() == 0 && group.countSubgroups() == 0){
                        metaContactListService.removeMetaContactGroup(metaContactListService.getRoot().
                                getMetaContactSubgroup(group.getGroupName()));
                    }
                }
            }
        }else {
            //No estamos conectados al core, simplemente eliminamos el contacto de la lista
            Iterator<MetaContact> it  = metaContactListService.getRoot().getChildContacts();
            while (it.hasNext()) {
                MetaContact mc = it.next();
                Iterator<Contact> itc = mc.getContacts();
                while (itc.hasNext()) {
                    Contact c = itc.next();
                    if (c.getAddress().equalsIgnoreCase(id)) {
                        metaContactListService.removeContact(c);
                        ContactGroup group = c.getParentContactGroup();
                        if (group.countContacts() == 0 && group.countSubgroups() == 0){
                            metaContactListService.removeMetaContactGroup(metaContactListService.getRoot().
                                    getMetaContactSubgroup(group.getGroupName()));
                        }
                    }
                }
            }
        }
    */
    }


    public void removeAllContacts() {

        if (xcapClient != null) {
            xcapClient.deleteDocument(DEFAULT_XCAP_FILE);
        }

    	/*if (metaContactListService == null) {
            logger.info("Contact List service not started");
            return;
        }
    	logger.debug("Eliminando todos los contactos");
    	
    	//Eliminamos todos los contactos de la lista
    	Iterator<MetaContactGroup> groupIt = metaContactListService.getRoot().getSubgroups();
    	while (groupIt.hasNext()) {
    		MetaContactGroup metaContactGroup = groupIt.next();
	        Iterator<MetaContact> it  = metaContactGroup.getChildContacts();
	        while (it.hasNext()) {
	            MetaContact mc = it.next();
	            Iterator<Contact> itc = mc.getContacts();
	            while (itc.hasNext()) {
	                Contact c = itc.next();
	                logger.debug("[PresenceService]Eliminando el contacto " + c.getAddress()
	                	+ " del grupo "  + c.getParentContactGroup().getGroupName());
	                metaContactListService.removeContact(c);
	            }
	        }
    		logger.debug("Eliminando el grupo " + metaContactGroup.getGroupName());
	        metaContactListService.removeMetaContactGroup(metaContactGroup);
    	}*/
    }

    
    
    
    public Map<String,PresenceInfo> getContactsInfo(){

        Map<String,PresenceInfo> m = new HashMap<String,PresenceInfo>();
        OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                getOperationSet(OperationSetPresence.class);
        if (presence != null) {
            Iterator contactsIt = presence.getServerStoredContactListRoot().contacts();
            while (contactsIt.hasNext()) {
                Contact c = (Contact)contactsIt.next();
                if (! c.getAddress().contains("-contacts")) {
                    m.put(c.getAddress(), new PresenceInfo(c));
                }
            }
        }
        return m;


        /*if (xcapClient != null) {
            return xcapClient.getContactsInfo(DEFAULT_XCAP_FILE);
        }else
            return null;*/
        /*if (metaContactListService == null) {
            logger.info("Contact List service not started");
            return null;
        }

        Iterator<MetaContactGroup> metaIt  = metaContactListService.getRoot().getSubgroups();
        Map<String,PresenceInfo> m = new HashMap<String,PresenceInfo>();
        while (metaIt.hasNext()) {
            MetaContactGroup mcg  = metaIt.next();
            Iterator<MetaContact> metaContactIt = mcg.getChildContacts();
            while (metaContactIt.hasNext()){
                MetaContact mc = metaContactIt.next();
                Iterator<Contact> contactsIt = mc.getContacts();
                while (contactsIt.hasNext()) {
                    Contact c = contactsIt.next();
                    m.put(c.getAddress(), new PresenceInfo(c));
                }
            }
        }
        return m;
        */
    }


    public void changeState(String newState){
        changeState(newState, null);
    }

    public void changeState(String newState, String note){
        OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                getOperationSet(OperationSetPresence.class);
        if (presence != null) {
            try {
                logger.debug("Cambiando el estado de presencia a " + newState);
                PresenceStatus ps = presence.containsState(newState);
                if (ps == null){
                    logger.debug("Añadiendo el estado de presencia " + newState);
                    ps = new SipStatusEnum(newState, note);
                }else if (note != null && ! note.equals(ps.getStatusDescription()))
                    ps.setStatusDescription(note);
                presence.publishPresenceStatus(ps, note);
            } catch (IllegalArgumentException ex) {
                logger.error(ex);
            } catch (IllegalStateException ex) {
                logger.error(ex);
            } catch (OperationFailedException ex) {
                logger.error(ex);
            }
        }

    }

    /**
     *
     */
    public void unregister() {
       if (protocolProviderService != null){
           OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                        getOperationSet(OperationSetPresence.class);
                if (presence != null) {
                    presence.unsubscribeToAllContact();
                }
           try{
                protocolProviderService.unregister();
            } catch (OperationFailedException ex) {
                logger.error("Error al desregistrar...");
                ex.printStackTrace();
            }
            /*try {
                OperationSetPresence presence = (OperationSetPresence)protocolProviderService.
                        getOperationSet(OperationSetPresence.class);
                if (presence != null) {
                    changeState("Offline");
                    Thread.sleep(FINAL_PUBLISH_PERIOD);
                }
                protocolProviderService.unregister();
            } catch (InterruptedException ex) {
                logger.error("Error al desregistrar...");
                ex.printStackTrace();
            } catch (OperationFailedException ex) {
                logger.error("Error al desregistrar...");
                ex.printStackTrace();
            }*/
       }
    }


    public void register() {
        try {
            if(! assertRegistered()){
                if (protocolProviderService != null){
                    protocolProviderService.register(new SecurityAuthorityImpl(protocolProviderService));
                }else {
                    logger.info("No existe ninguna cuenta de usuario configurada");
                }
            }
        }catch (OperationFailedException ex) {
            logger.error("[PresenceServiceImpl]sError al intentar registrarse");
        }
    }


    public String getPresenceState() {
        OperationSetPresence presence = (OperationSetPresence) protocolProviderService
                .getOperationSet(OperationSetPresence.class);
            if (presence != null)
                return presence.getCurrentStatusMessage();
            else
                return null;
    }

     /**
     *
     * @return
     */
    private boolean assertRegistered(){
        if (protocolProviderService == null || ! protocolProviderService.isRegistered()){
            return false;
        }
        return true;
    }
}
