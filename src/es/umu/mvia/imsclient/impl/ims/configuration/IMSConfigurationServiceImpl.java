/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package es.umu.mvia.imsclient.impl.ims.configuration;

import es.umu.mvia.imsclient.service.ims.configuration.AccountInfo;
import java.io.*;
import java.util.*;

import es.umu.mvia.imsclient.impl.protocol.sip.ProtocolProviderFactorySipImpl;
import es.umu.mvia.imsclient.service.ims.configuration.IMSConfigurationService;
import es.umu.mvia.imsclient.service.protocol.*;
import es.umu.mvia.imsclient.util.Logger;

/**
 *
 * @author Usuario
 */
public class IMSConfigurationServiceImpl implements IMSConfigurationService {
    
    private Logger logger = Logger.getLogger(IMSConfigurationServiceImpl.class);
    private static ProtocolProviderFactorySipImpl protocolProviderFactory = null;

    /*Default Configuration values*/
    private final Map<String,String> defaultValues = new HashMap<String,String>();

    private final String PROPERTIES_FILE_NAME = "default.properties";


    private final String TRACE_LEVEL = "gov.nist.javax.sip.TRACE_LEVEL";


    public IMSConfigurationServiceImpl(){
        initialize();
        protocolProviderFactory = new ProtocolProviderFactorySipImpl();
    }

    public void initialize() {

        Properties props = loadConfiguration(".//" + PROPERTIES_FILE_NAME);

        if (props == null)
            props = loadConfiguration(System.getProperty("user.dir") +
                System.getProperty("file.separator") + PROPERTIES_FILE_NAME);
        
        if (props == null) {
        	defaultValues.put(ProtocolProviderFactory.PROXY_ADDRESS, "195.235.93.158");
            defaultValues.put(ProtocolProviderFactory.SERVER_ADDRESS, "open-ims.test");
            defaultValues.put(ProtocolProviderFactory.SERVER_PORT, "5060");
            defaultValues.put(ProtocolProviderFactory.PROXY_PORT, "4060");
            defaultValues.put(ProtocolProviderFactory.PREFERRED_TRANSPORT, "UDP");
            defaultValues.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED, "true");
            defaultValues.put(ProtocolProviderFactory.FORCE_P2P_MODE, "false");
            defaultValues.put(ProtocolProviderFactory.POLLING_PERIOD, "60");
            defaultValues.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION, "30000");
        } else {
        
            defaultValues.put(ProtocolProviderFactory.PROXY_ADDRESS,
                    props.getProperty("PROXY_ADDRESS"));

            defaultValues.put(ProtocolProviderFactory.SERVER_ADDRESS,
                    props.getProperty("SERVER_ADDRESS"));

            defaultValues.put(ProtocolProviderFactory.SERVER_PORT,
                    props.getProperty("SERVER_PORT"));

            defaultValues.put(ProtocolProviderFactory.PROXY_PORT,
                    props.getProperty("PROXY_PORT"));

            defaultValues.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                    props.getProperty("PREFERRED_TRANSPORT"));

            defaultValues.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                    props.getProperty("IS_PRESENCE_ENABLED"));

            defaultValues.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                    props.getProperty("FORCE_P2P_MODE"));

            defaultValues.put(ProtocolProviderFactory.POLLING_PERIOD,
                    props.getProperty("POLLING_PERIOD"));

            defaultValues.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                    props.getProperty("SUBSCRIPTION_EXPIRATION"));
            
            if (props.containsKey(TRACE_LEVEL)) {
                defaultValues.put(TRACE_LEVEL, props.getProperty(TRACE_LEVEL));
            }

        }
    }

    private Properties loadConfiguration(String path) {
    	// Read properties file.
        Properties properties = new Properties();
        try {
            logger.info("Loading IMS Client properties from " + path);
        	properties.load(new FileInputStream(path));
            logger.info("IMS Client properties correctly loaded from " + path);
        } catch (IOException e) {
        	logger.error("Error loading proxy properties. Trying to use default configuration file");
            String defaultPath = System.getProperty("user.home") + 
                    System.getProperty("file.separator") + PROPERTIES_FILE_NAME;
            try {
                logger.info("Loading IMS Client properties from " + defaultPath);
                properties.load(new FileInputStream(defaultPath));
                logger.info("IMS Client properties correctly loaded from " + defaultPath);
            } catch (IOException ex) {
                return null;
            }
        }        
        return properties;
    }


            /* Implementation of the IMS Configuration Service methods*/
    /**
     *  Crea una nueva cuenta IMS. Borra cualquier cuenta existente y crea una nueva
     * @param properties
     */
    public ProtocolProviderService newAccount(String userID, String password, Map<String, String> accountProperties) {
        if (! assertFactory()){
            logger.error("[IMSConfigurationServiceImpl] Protocol Provider Factory not found");
            return null;
        }

        //Tenemos un cliente monopuesto, asi que se eliminara cualquier otra cuenta activa
        deleteAccount();

        accountProperties.put(ProtocolProviderFactory.PASSWORD, password);

        setDefaultParam(accountProperties, ProtocolProviderFactory.SERVER_ADDRESS);
        setDefaultParam(accountProperties, ProtocolProviderFactory.SERVER_PORT);
        setDefaultParam(accountProperties, ProtocolProviderFactory.PROXY_ADDRESS);
        setDefaultParam(accountProperties, ProtocolProviderFactory.PROXY_PORT);
        setDefaultParam(accountProperties, ProtocolProviderFactory.PREFERRED_TRANSPORT);
        setDefaultParam(accountProperties, ProtocolProviderFactory.IS_PRESENCE_ENABLED);
        setDefaultParam(accountProperties, ProtocolProviderFactory.FORCE_P2P_MODE);
        setDefaultParam(accountProperties, ProtocolProviderFactory.POLLING_PERIOD);
        setDefaultParam(accountProperties, ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION);
        setDefaultParam(accountProperties, TRACE_LEVEL);
        
        try {
            AccountID accountID = protocolProviderFactory.
                    installAccount(userID, accountProperties);
            return protocolProviderFactory.getProviderForAccount(accountID);
        }catch (IllegalArgumentException exc){
            logger.error(exc.getMessage());
            return null;
        }catch (IllegalStateException exc){
            logger.info("Account for user " + userID + " is already installed");
            return null;
        }
    }

    
    /**
     *
     */
    public void deleteAccount() {
        if (! assertFactory()){
            logger.error("[IMSConfigurationServiceImpl] Protocol Provider Service not found");
            return;
        }

        Iterator it = protocolProviderFactory.getRegisteredAccounts().iterator();
        while (it.hasNext()){
            AccountID ac = (AccountID)it.next();
            logger.info("[IMSConfigurationServiceImpl] Uninstalling " + ac.getUserID() + "account...");
            protocolProviderFactory.uninstallAccount(ac);
        }
    }

    /**
     *
     */
    public AccountInfo getAccountInfo() {
        if (! assertFactory()){
            logger.error("[IMSConfigurationServiceImpl] Protocol Provider Service not found");
            return null;
        }

        Iterator<AccountID> it = protocolProviderFactory.getRegisteredAccounts().iterator();
        if (it.hasNext()){
            ProtocolProviderService pps = (ProtocolProviderService) protocolProviderFactory.
                    getProviderForAccount(it.next());
            OperationSetPresence presence = (OperationSetPresence) pps
                .getOperationSet(OperationSetPresence.class);
            if (presence != null) {
                PresenceStatus ps = presence.getLocalContact().getPresenceStatus();
                return new AccountInfo(pps.getAccountID().toString(),
                        pps.getRegistrationState().getStateName(),
                        ps.getStatusName(),
                        ps.getStatusDescription());
            }else
                return new AccountInfo(pps.getAccountID().getAccountAddress(),
                        pps.getRegistrationState().getStateName());

        }else
            return null;
    }


    /**
     *
     * @return
     */
    public ProtocolProviderService getProtocolProvider() {
        Iterator<AccountID> it = protocolProviderFactory.getRegisteredAccounts().iterator();
        if (it.hasNext())
            return protocolProviderFactory.getProviderForAccount(it.next());
        else
            return null;
    }

    public ProtocolProviderFactory getProtocolProviderFactory() {
        return protocolProviderFactory;
    }

    public static ProtocolProviderFactorySipImpl getProtocolProviderFactorySipImpl() {
        return protocolProviderFactory;
    }

    
    /**
     *
     * @return
     */
    private boolean assertFactory(){
        if (protocolProviderFactory == null){
            return false;
        }
        return true;
    }

    /**
     * 
     * @param m
     * @param key
     */
    private void setDefaultParam(Map<String,String> m, String key){
        if (m.get(key) == null) {
            m.put(key, defaultValues.get(key));
        }
    }

}
