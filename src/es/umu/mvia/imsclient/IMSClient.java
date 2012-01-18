/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient;


import es.umu.mvia.imsclient.service.protocol.OperationFailedException;
import es.umu.mvia.imsclient.service.protocol.event.ContactPresenceStatusChangeEvent;
import es.umu.mvia.imsclient.service.protocol.event.MessageDeliveredEvent;
import es.umu.mvia.imsclient.service.protocol.event.MessageDeliveryFailedEvent;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import es.umu.mvia.imsclient.impl.ims.configuration.IMSConfigurationServiceImpl;
import es.umu.mvia.imsclient.impl.ims.message.IMSMessageServiceImpl;
import es.umu.mvia.imsclient.impl.ims.presence.IMSPresenceServiceImpl;
import es.umu.mvia.imsclient.impl.protocol.sip.OperationSetBasicTelephonySipImpl;
import es.umu.mvia.imsclient.impl.protocol.sip.ProtocolProviderServiceSipImpl;
import es.umu.mvia.imsclient.service.ims.configuration.AccountInfo;
import es.umu.mvia.imsclient.service.ims.configuration.IMSConfigurationService;
import es.umu.mvia.imsclient.service.ims.message.IMSMessageService;
import es.umu.mvia.imsclient.service.ims.presence.IMSPresenceService;
import es.umu.mvia.imsclient.service.ims.presence.PresenceInfo;
import es.umu.mvia.imsclient.service.protocol.OperationSetBasicInstantMessaging;
import es.umu.mvia.imsclient.service.protocol.OperationSetPresence;
import es.umu.mvia.imsclient.service.protocol.ProtocolProviderFactory;
import es.umu.mvia.imsclient.service.protocol.ProtocolProviderService;
import es.umu.mvia.imsclient.service.protocol.event.ContactPresenceStatusListener;
import es.umu.mvia.imsclient.service.protocol.event.MessageListener;
import es.umu.mvia.imsclient.service.protocol.event.MessageReceivedEvent;
import es.umu.mvia.imsclient.impl.protocol.sip.SipRegistrarConnection;
import es.umu.mvia.imsclient.service.protocol.Call;
import es.umu.mvia.imsclient.service.protocol.OperationSetBasicTelephony;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sip.ClientTransaction;
import javax.sip.ListeningPoint;
import javax.sip.SipException;
import javax.sip.TransactionUnavailableException;
import javax.sip.message.Request;


/**
 *
 * @author Usuario
 */
public class IMSClient  implements MessageListener, ContactPresenceStatusListener {

    private IMSPresenceService presenceService = null;
    private IMSConfigurationService configurationService = null;
    private IMSMessageService messageService = null;
    private ProtocolProviderService protocolProviderService = null;

    private SipRegistrarConnection sipRegistarConnection = null;

    private List<IMSMessageListener> messageListeners = new ArrayList<IMSMessageListener>();
    private List<IMSPresenceListener> presenceListeners = new ArrayList<IMSPresenceListener>();


    private int state = IMSState.NO_ACCOUNT;
    private String DEFAULT_OFFLINE_PRESENCE_STATUS = "Offline";

    public IMSClient() {
        //Añadido para pruebas///
        sipRegistarConnection = new SipRegistrarConnection();

        configurationService = new IMSConfigurationServiceImpl();

        protocolProviderService = configurationService.getProtocolProvider();
        if (protocolProviderService != null) {
            state = IMSState.ACCOUNT_INSTALLED;
            presenceService = new IMSPresenceServiceImpl(protocolProviderService,
                    configurationService.getProtocolProviderFactory());
        }
    }

    
                /*IMS Configuration methods*/

    private void newAccount(String userID, String passwd, Map<String,String> accProps) {

        if (state != IMSState.NO_ACCOUNT) {
            try {
                deleteAccount();
            } catch (IMSIllegalStateException ex) {}
        }

        protocolProviderService = configurationService.newAccount(userID, passwd, accProps);
        presenceService = new IMSPresenceServiceImpl(protocolProviderService,
                configurationService.getProtocolProviderFactory());
        state = IMSState.ACCOUNT_INSTALLED;
    }

    public void newAccount(String userID, String passwd) {
        newAccount(userID, passwd, new HashMap());
    }

    public void newAccount(String userID, String passwd, String proxyAddr) {
        Map<String,String> accProps = new HashMap();
        accProps.put(ProtocolProviderFactory.PROXY_ADDRESS, proxyAddr);
        newAccount(userID, passwd, accProps);
    }

    public void deleteAccount() throws IMSIllegalStateException {

        if (state == IMSState.NO_ACCOUNT)
            throw new IMSIllegalStateException("There is not an installed account.");
        if (state == IMSState.REGISTERED)
            unregister();
        
        configurationService.deleteAccount();
        presenceService = null;
        state = IMSState.NO_ACCOUNT;
    }


    public AccountInfo getAccountInfo() throws IMSIllegalStateException {
        if (state != IMSState.NO_ACCOUNT)
            return configurationService.getAccountInfo();
        else
            throw new IMSIllegalStateException("There is not an installed account.");
    }



                    /* IMS Presence methods*/

    public void addPresenceListener(IMSPresenceListener listener) {
        presenceListeners.add(listener);
    }

    public void removePresenceListener(IMSPresenceListener listener) {
        presenceListeners.remove(listener);
    }

    public boolean isRegistered() {
        return state == IMSState.REGISTERED;
    }

    public void register() throws IMSIllegalStateException {
        if (state == IMSState.ACCOUNT_INSTALLED) {
            presenceService.register();
            state = IMSState.REGISTERED;
            messageService = new IMSMessageServiceImpl(configurationService.getProtocolProvider(),
                    presenceService);
            OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging)
                    protocolProviderService.getOperationSet(OperationSetBasicInstantMessaging.class);
            im.addMessageListener(this);
            
            OperationSetPresence pres = (OperationSetPresence)
                    protocolProviderService.getOperationSet(OperationSetPresence.class);
            pres.addContactPresenceStatusListener(this);

        }else
            throw new IMSIllegalStateException("There is not an installed account.");
    }

    public void unregister() throws IMSIllegalStateException {
        if (state == IMSState.REGISTERED) {
            OperationSetBasicInstantMessaging im = (OperationSetBasicInstantMessaging)
                    protocolProviderService.getOperationSet(OperationSetBasicInstantMessaging.class);
            if (im != null)
                im.removeMessageListener(this);

            OperationSetPresence pres = (OperationSetPresence)
                    protocolProviderService.getOperationSet(OperationSetPresence.class);
            if (pres != null)
                pres.removeContactPresenceStatusListener(this);

            presenceService.unregister();
            messageService = null;
            state = IMSState.ACCOUNT_INSTALLED;
        } else
            throw new IMSIllegalStateException("The IMS client is not registered.");
    }


    public void addContact(String userID) throws IMSIllegalStateException {
        addContact(userID, null);
    }

    public void addContact(String userID, String grupo) throws IMSIllegalStateException{
        if (state != IMSState.NO_ACCOUNT)
            presenceService.addContact(userID, grupo);
        else
            throw new IMSIllegalStateException("There is not an installed account");
    }
    
    public void removeContact(String userID) throws IMSIllegalStateException {
        if (state != IMSState.NO_ACCOUNT)
            presenceService.removeContact(userID);
        else
            throw new IMSIllegalStateException("There is not an installed account.");
    }

    public void setNewState(String newState) throws IMSIllegalStateException {
        setNewState(newState.trim(), null);
    }

    public void setNewState(String newState, String note) throws IMSIllegalStateException {
        if (state == IMSState.REGISTERED)
            presenceService.changeState(newState, note);
        else
            throw new IMSIllegalStateException("The IMS client must be registered for changing its presence status");
    }


    public String getPresenceState() {
        if (state == IMSState.REGISTERED) {
            return presenceService.getPresenceState();
        }else
            return DEFAULT_OFFLINE_PRESENCE_STATUS;
    }

    public Map<String,PresenceInfo> getContactsInfo() throws IMSIllegalStateException {
        if (state != IMSState.NO_ACCOUNT)
            return presenceService.getContactsInfo();
        else
            throw new IMSIllegalStateException("There is not an installed account.");
    }


    
                /* IMS Message methods*/

    public void addMessageListener(IMSMessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(IMSMessageListener listener) {
        messageListeners.remove(listener);
    }

    public void sendMessage(String dest, String text) throws IMSIllegalStateException {
        if (state == IMSState.REGISTERED)
            messageService.sendMessage(dest, text);
        else
            throw new IMSIllegalStateException("The IMS client must be registered for sending messages");
    }

    

                /* IMS Message Listener Interface methods*/
    
    public void messageReceived(MessageReceivedEvent event) {
        if (event != null && messageListeners != null && state == IMSState.REGISTERED) {
            for (IMSMessageListener listener : messageListeners)
                listener.receiveMessage(event.getSourceContact().getAddress(),
                                event.getSourceMessage().getContent());
        }
    }

    public void messageDelivered(MessageDeliveredEvent evt) {}

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {}

    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent event) {
        if (event != null && presenceListeners != null && state == IMSState.REGISTERED) {
            for (IMSPresenceListener listener : presenceListeners) {
                listener.contactStateChanged(event.getSourceContact().getAddress(),
                        new PresenceInfo(event.getSourceContact()));
            }
        }
    }

    public void closeClientIMS(){
        System.out.println("Finalizando cliente IMS...");
        System.exit(0);
    }

    public long getTiempoFinRegistro(){
        return protocolProviderService.getTiempoFinRegistro();
    }

    public long  getTiempoInicioRegistro(){
        return this.sipRegistarConnection.getTiempoInicioRegistro();
    }

    public long  getTiempoInicioDesRegistro(){
        return this.sipRegistarConnection.getTiempoInicioDesregistro();
    }

    public void sendInvite() throws TransactionUnavailableException, SipException, OperationFailedException, UnknownHostException {

        OperationSetBasicTelephony basicTelephony = (OperationSetBasicTelephony)
            protocolProviderService.getOperationSet(OperationSetBasicTelephony.class);
        if (basicTelephony != null) {
            System.out.println("basicTelephony: " + basicTelephony.toString());
            try {
                Call call = basicTelephony.createCall("urn:service:sos");
                //Call call = basicTelephony.createCall("alice");

            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendInvite(String party) throws TransactionUnavailableException, SipException, OperationFailedException, UnknownHostException {

        OperationSetBasicTelephony basicTelephony = (OperationSetBasicTelephony)
            protocolProviderService.getOperationSet(OperationSetBasicTelephony.class);
        if (basicTelephony != null) {
            System.out.println("basicTelephony: " + basicTelephony.toString());
            try {
                Call call = basicTelephony.createCall(party);
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }

    }

}
