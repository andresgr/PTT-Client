/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol;

import java.util.*;

import es.umu.mvia.imsclient.service.protocol.event.*;

/**
 * OperationSetPresence offers methods for
 * @todo Say that ContactList management is else where and Presence functions
 * are here
 * @todo Say that presence implementations should not contain any persistant
 * data of subscriptions
 * @todo say that this is not persistent and that persistent is elsewhere.
 *
 * @author Emil Ivov
 */
public interface OperationSetPresence
    extends OperationSet
{

    /**
     * Returns a PresenceStatus instance representing the state this provider is
     * currently in. Note that PresenceStatus instances returned by this method
     * MUST adequately represent all possible states that a provider might
     * enter duruing its lifecycle, includindg those that would not be visible
     * to others (e.g. Initializing, Connecting, etc ..) and those that will be
     * sent to contacts/buddies (On-Line, Eager to chat, etc.).
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus();


    
    /**
     * Returns the ContactGroup list
     * @return
     */
    public ContactGroup getServerStoredContactListRoot();

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters. Note that calling this method does not necessarily
     * imply that the requested status would be entered. This method would
     * return right after being called and the caller should add itself as
     * a listener to this class in order to get notified when the state has
     * actually changed.
     *
     * @param status the PresenceStatus as returned by getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     * enter that status
     * @throws IllegalArgumentException if the status requested is not a valid
     * PresenceStatus supported by this provider.
     * @throws java.lang.IllegalStateException if the provider is not currently
     * registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if publishing
     * the status fails due to a network error.
     */
    public void publishPresenceStatus(PresenceStatus status, String statusMessage)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter. Note that the provider would most
     * probaby enter more states than those returned by this method as they
     * only depict instances that users may request to enter. (e.g. a user
     * may not request a "Connecting..." state - it is a temporary state
     * that the provider enters while trying to enter the "Connected" state).
     *
     * @return Iterator a PresenceStatus array containing "enterable"
     * status instances.
     */
    public Iterator getSupportedStatusSet();

    /**
     * Get the PresenceStatus for a particular contact. This method is not meant
     * to be used by the user interface (which would simply register as a
     * presence listener and always follow contact status) but rather by other
     * plugins that may for some reason need to know the status of a particular
     * contact.
     * <p>
     * @param contactIdentifier the identifier of the contact whose status we're
     * interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     * <tt>contact</tt>
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if retrieving
     * the status fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;

    /**
     * Adds a subscription for the presence status of the contact corresponding
     * to the specified contactIdentifier. Note that apart from an exception in
     * the case of an immediate failure, the method won't return any indication
     * of success or failure. That would happen later on through a
     * SubscriptionEvent generated by one of the methods of the
     * SubscriptionListener.
     * <p>
     * This subscription is not going to be persistent (as opposed to
     * subscriptions added from the OperationSetPersistentPresence.subscribe()
     * method)
     * @param contactIdentifier the identifier of the contact whose status
     * updates we are subscribing for.
     * <p>
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;

    /**
     * Removes a subscription for the presence status of the specified contact.
     * @param contact the contact whose status updates we are unsubscribing from.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if unsubscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;


    /**
      * Unsubscribe to every contact.
      */
     public void unsubscribeToAllContact();



    /**
     * Returns a reference to the contact with the specified ID in case we have
     * a subscription for it and null otherwise/
     * @param contactID a String identifier of the contact which we're seeking a
     * reference of.
     * @return a reference to the Contact with the specified
     * <tt>contactID</tt> or null if we don't have a subscription for the
     * that identifier.
     */
    public Contact findContactByID(String contactID);

    /**
     * Returns the protocol specific contact instance representing the local
     * user. In the case of SIP this would be your local sip address or in the
     * case of an IM protocol such as ICQ - your own uin. No set method should
     * be provided in implementations of this class. The getLocalContact()
     * method is only used for giving information to the user on their currently
     * used addressed a different service (ConfigurationService) should be used
     * for changing that kind of settings.
     * @return the Contact (address, phone number, or uin) that the Provider
     * implementation is communicating on behalf of.
     */
    public Contact getLocalContact();

    /**
     * Handler for incoming authorization requests. An authorization request
     * notifies the user that someone is trying to add her to their contact list
     * and requires her to approve or reject authorization for that action.
     * @param handler an instance of an AuthorizationHandler for authorization
     * requests coming from other users requesting permission add us to their
     * contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler);

    /**
     * Adds a listener that would receive events upon changes of the provider
     * presence status.
     * @param listener the listener to register for changes in our PresenceStatus.
     */
    public void addProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener);

    /**
     * Unregisters the specified listener so that it does not receive further
     * events upon changes in local presence status.
     * @param listener ProviderPresenceStatusListener
     */
    public void removeProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener);

    /**
     * Registers a listener that would receive a presence status change event
     * every time a contact, whose status we're subscribed for, changes her
     * status.
     * Note that, for reasons of simplicity and ease of implementation, there
     * is only a means of registering such "global" listeners that would receive
     * updates for status changes for any contact and it is not currently
     * possible to register such contacts for a single contact or a subset of
     * contacts.
     *
     * @param listener the listener that would received presence status
     * updates for contacts.
     */
    public void addContactPresenceStatusListener(
        ContactPresenceStatusListener listener);

    /**
     * Removes the specified listener so that it won't receive any further
     * updates on contact presence status changes
     * @param listener the listener to remove.
     */
    public void removeContactPresenceStatusListener(
        ContactPresenceStatusListener listener);

    /**
     * Registers a listener that would get notifications any time a new
     * subscription was succesfully added, has failed or was removed.
     * @param listener the SubscriptionListener to register
     */
    public void addSubsciptionListener(SubscriptionListener listener);

    /**
     * Removes the specified subscription listener.
     * @param listener the listener to remove.
     */
    public void removeSubscriptionListener(SubscriptionListener listener);

    /**
     * Returns the status message that was confirmed by the serfver
     * @return the last status message that we have requested and the aim server
     * has confirmed.
     */
    public String getCurrentStatusMessage();

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     *
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(
        String address, String persistentData);

    /**
     * Returns the PresenceStatus with <tt>newState</tt> name
     * @param newState
     * @return
     */
    public PresenceStatus containsState(String newState);


}
