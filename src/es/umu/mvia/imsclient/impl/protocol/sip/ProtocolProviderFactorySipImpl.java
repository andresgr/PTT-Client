/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.impl.protocol.sip;

import java.util.*;

import es.umu.mvia.imsclient.service.protocol.*;
import es.umu.mvia.imsclient.util.*;

/**
 * A SIP implementation of the protocol provider factory interface.
 *
 * @author Emil Ivov
 */
public class ProtocolProviderFactorySipImpl
    extends ProtocolProviderFactory
{
    private static final Logger logger =
        Logger.getLogger(ProtocolProviderFactorySipImpl.class);

    /**
     * The table that we store our accounts in.
     */
    private Hashtable registeredAccounts = new Hashtable();

    /**
     * Constructs a new instance of the ProtocolProviderFactorySipImpl.
     */
    public ProtocolProviderFactorySipImpl()
    {
        logger.debug("ProtocolProviderFactorySipImpl Started.");

        loadStoredAccounts();
    }

    /**
     * Returns the ServiceReference for the protocol provider corresponding
     * to the specified accountID or null if the accountID is unknown.
     *
     * @param accountID the accountID of the protocol provider we'd like to
     *   get
     * @return a ServiceReference object to the protocol provider with the
     *   specified account id and null if the account id is unknwon to the
     *   provider factory.
     */
    public ProtocolProviderService getProviderForAccount(AccountID accountID)
    {
        return (ProtocolProviderService)registeredAccounts.get(accountID);
    }

    /**
     * Returns a copy of the list containing all accounts currently
     * registered in this protocol provider.
     *
     * @return a copy of the llist containing all accounts currently installed
     * in the protocol provider.
     */
    public ArrayList getRegisteredAccounts()
    {
        return new ArrayList(registeredAccounts.keySet());
    }

    /**
     * Initializaed and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr the user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     * @throws IllegalArgumentException if userID does not correspond to an
     *   identifier in the context of the underlying protocol or if
     *   accountProperties does not contain a complete set of account
     *   installation properties.
     * @throws IllegalStateException if the account has already been
     *   installed.
     * @throws NullPointerException if any of the arguments is null.
     */
    public AccountID installAccount( String userIDStr,
                                 Map accountProperties)
    {
        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        accountProperties.put(USER_ID, userIDStr);

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        String serverAddress = (String)accountProperties.get(SERVER_ADDRESS);

        if(serverAddress == null)
            throw new NullPointerException(
                        serverAddress
                        + " is not a valid ServerAddress");

        AccountID accountID =
            new SipAccountID(userIDStr, accountProperties, serverAddress);

        //make sure we haven't seen this account id before.
        if( registeredAccounts.containsKey(accountID) )
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");


        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (trhgough the UI) a call to
        //the register() method and it needs to acces the configuration service
        //and check for a password.
        this.storeAccount(accountID);

        try
        {
            accountID = loadAccount(accountProperties);
        }
        catch(RuntimeException exc)
        {
            //it might happen that load-ing the account fails because of a bad
            //initialization. if this is the case, make sure we remove it.
            this.removeStoredAccount(accountID);

            throw exc;
        }

        return accountID;
    }


    /**
     * Initializes and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account
     */
    protected AccountID loadAccount(Map accountProperties)
    {
        String userIDStr = (String)accountProperties.get(USER_ID);
        if(userIDStr == null)
            throw new NullPointerException(
                "The account properties contained no user id.");

        if(accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        String serverAddress = (String)accountProperties.get(SERVER_ADDRESS);

        if(serverAddress == null)
            throw new NullPointerException(
                        serverAddress
                        + " is not a valid ServerAddress");

        AccountID accountID =
            new SipAccountID(userIDStr, accountProperties, serverAddress);

        //get a reference to the configuration service and register whatever
        //properties we have in it.

        ProtocolProviderServiceSipImpl sipProtocolProvider
            = new ProtocolProviderServiceSipImpl();

        try
        {
            sipProtocolProvider.initialize(userIDStr, accountID);
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to initialize account", ex);
            throw new IllegalArgumentException("Failed to initialize account"
                + ex.getMessage());
        }

        registeredAccounts.put(accountID, sipProtocolProvider);
        return accountID;
    }

    /**
     * Loads (and hence installs) all accounts previously stored in the
     * configuration service.
     */
    public void loadStoredAccounts()
    {
        super.loadStoredAccounts();
    }


    /**
     * Removes the specified account from the list of accounts that this
     * provider factory is handling. If the specified accountID is unknown to
     * the ProtocolProviderFactory, the call has no effect and false is returned.
     * This method is persistent in nature and once called the account
     * corresponding to the specified ID will not be loaded during future runs
     * of the project.
     *
     * @param accountID the ID of the account to remove.
     * @return true if an account with the specified ID existed and was removed
     * and false otherwise.
     */
    public boolean uninstallAccount(AccountID accountID)
    {
        //unregister the protocol provider
        ProtocolProviderService protocolProvider = getProviderForAccount(accountID);

        try {
            protocolProvider.unregister();
        }
        catch (OperationFailedException e) {
            logger.error("Failed to unregister protocol provider for account : "
                    + accountID + " caused by : " + e);
        }

        ProtocolProviderService registration = 
                (ProtocolProviderService) registeredAccounts.remove(accountID);

        if (registration == null)
            return false;

        try {
            //kill the service
            registration.unregister();
        } catch (OperationFailedException ex) {
            ex.printStackTrace();
        }

        return removeStoredAccount(accountID);
    }

    /**
     * Prepares the factory for bundle shutdown.
     */
    public void stop()
    {
        logger.trace("Preparing to stop all SIP protocol providers.");
        Enumeration registrations = this.registeredAccounts.elements();

        while(registrations.hasMoreElements())
        {
            ProtocolProviderServiceSipImpl provider
                = (ProtocolProviderServiceSipImpl) registrations.nextElement();

            //do an attempt to kill the provider
            provider.shutdown();
        }

        registeredAccounts.clear();
    }

    /**
     * Saves the password for the specified account after scrambling it a bit
     * so that it is not visible from first sight (Method remains highly
     * insecure).
     *
     * @param accountID the AccountID for the account whose password we're
     * storing.
     * @param passwd the password itself.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public void storePassword(AccountID accountID, String passwd)
        throws IllegalArgumentException
    {
        super.storePassword(accountID, passwd);
    }

    /**
     * Returns the password last saved for the specified account.
     *
     * @param accountID the AccountID for the account whose password we're
     * looking for..
     *
     * @return a String containing the password for the specified accountID.
     *
     * @throws java.lang.IllegalArgumentException if no account corresponding
     * to <tt>accountID</tt> has been previously stored.
     */
    public String loadPassword(AccountID accountID)
        throws IllegalArgumentException
    {
        return super.loadPassword( accountID );
    }

}
