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
 * Represents a chat channel/room/rendez-vous point/ where multiple chat users
 * could rally and communicate in a many-to-many fashion.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface ChatRoom
{
    /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName();
    
    /**
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public String getIdentifier();

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join()
        throws OperationFailedException;

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. The method uses the nickname of the local user and the
     * specified password in order to enter the chatroom.
     *
     * @param password the password to use when authenticating on the chatroom.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join(byte[] password)
        throws OperationFailedException;

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chatroom already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname)
        throws OperationFailedException;

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     * room.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException;

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    public boolean isJoined();

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave();

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomChangelistener</tt> to this room.
     * {@link #addPropertyChangeListener(ChatRoomPropertyChangeListener)}
     * <p>
     * To change the room's subject use {@link #setSubject(String)}.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     * room or the room does not have a subject yet.
     */
    public String getSubject();

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException if the user doesn't have the right to
     * change this property.
     */
    public void setSubject(String subject)
        throws OperationFailedException;

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getUserNickname();

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * 
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the new nickname already exist in
     * this room
     */
    public void setUserNickname(String nickname)
       throws OperationFailedException;

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     * 
     * @param listener a member participation listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     * 
     * @param listener a member participation listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener);

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener listener);

    /**
     * Removes a listener that was being notified of changes in our role in this
     * chat room such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener);

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener listener);

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener);

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     * 
     * @param listener a property change listener.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener);

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     * 
     * @param listener a property change listener.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener);

    /**
     * Adds a listener that will be notified of changes in the property of a
     * room member such as the nickname being changed.
     * 
     * @param listener a room member property change listener.
     */
    public void addMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener);

    /**
     * Removes a listener that was being notified of changes in the property of
     * a chat room member such as the nickname being changed.
     * 
     * @param listener a room member property change listener.
     */
    public void removeMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener);

    /**
     * Invites another user to this room.
     * <p>
     * If the room is password-protected, the invitee will receive a password to
     * use to join the room. If the room is members-only, the the invitee may
     * be added to the member list.
     *
     * @param userAddress the address of the user to invite to the room.(one
     * may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell the
     * the user why they are being invited.
     */
    public void invite(String userAddress, String reason);

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     * @throws OperationFailedException if we fail retrieving the list of room
     * participants.
     */
    public List getMembers();

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    public int getMembersCount();

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener);

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener);

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject);

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText);

    /**
     * Sends the <tt>message</tt> to this chat room.
     * 
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message message)
        throws OperationFailedException;

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider();

    /**
     * Returns an Iterator over a set of ban masks for this chat room. The ban
     * mask defines a group of users that will be banned. The ban list is a list
     * of all such ban masks defined for this chat room.
     * 
     * @return an Iterator over a set of ban masks for this chat room
     * @throws OperationFailedException if an error occured while performing the
     * request to the server or you don't have enough privileges to get this
     * information
     */
    public Iterator getBanList() throws OperationFailedException;
    
    /**
     * Bans a user from the room. An administrator or owner of the room can ban
     * users from a room. A banned user will no longer be able to join the room
     * unless the ban has been removed. If the banned user was present in the
     * room then he/she will be removed from the room and notified that he/she
     * was banned along with the reason (if provided) and the user who initiated
     * the ban.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException if an error occurs while banning a user.
     * In particular, an error can occur if a moderator or a user with an
     * affiliation of "owner" or "admin" was tried to be banned or if the user
     * that is banning have not enough permissions to ban.
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;
    
    /**
     * Kicks a visitor or participant from the room.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     * room
     * @throws OperationFailedException if an error occurs while kicking the
     * participant. In particular, an error can occur if a moderator or a user
     * with an affiliation of "owner" or "administrator" was intended to be
     * kicked; or if the participant that intended to kick another participant
     * does not have kicking privileges;
     */
    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;
    
    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. If the user doesn't have
     * permissions to see and change chat room configuration an
     * <tt>OperationFailedException</tt> is thrown. 
     * 
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     * @throws OperationFailedException if the user doesn't have
     * permissions to see and change chat room configuration
     */
    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException;

    /**
     * Returns <code>true</code> if this chat room is a system room and 
     * <code>false</code> otherwise.
     * 
     * @return <code>true</code> if this chat room is a system room and 
     * <code>false</code> otherwise.
     */
    public boolean isSystem();
}