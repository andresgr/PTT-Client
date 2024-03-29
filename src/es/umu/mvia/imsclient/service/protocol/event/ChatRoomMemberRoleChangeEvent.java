/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol.event;

import java.util.*;

import es.umu.mvia.imsclient.service.protocol.*;

/**
 * Dispatched to notify interested parties that a change in a member role in the
 * source room has occurred. Changes may include member being granted admin
 * permissions, or other permissions.
 * 
 * @see ChatRoomMemberRole
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomMemberRoleChangeEvent
    extends EventObject
{
    /**
     * The member that the event relates to.
     */
    private ChatRoomMember sourceMember = null;
    
    /**
     * The previous role that this member had.
     */
    private ChatRoomMemberRole previousRole = null;
    
    /**
     * The new role that this member get.
     */
    private ChatRoomMemberRole newRole = null;
    
    /**
     * Creates a <tt>ChatRoomMemberRoleChangeEvent</tt> representing that
     * a change in member role in the source chat room has occured.
     * 
     * @param sourceRoom the <tt>ChatRoom</tt> that produced this event
     * @param sourceMember the <tt>ChatRoomMember</tt> that this event is about
     * @param previousRole the previous role that member had
     * @param newRole the new role that member get
     */
    public ChatRoomMemberRoleChangeEvent(ChatRoom sourceRoom,
                                        ChatRoomMember sourceMember,
                                        ChatRoomMemberRole previousRole,
                                        ChatRoomMemberRole newRole)                                     
    {
        super(sourceRoom);
        this.sourceMember = sourceMember;
        this.previousRole = previousRole;
        this.newRole = newRole;
    }
    
    /**
     * Returns the new role given to the member that this event is about.
     *
     * @return the new role given to the member that this event is about
     */
    public ChatRoomMemberRole getNewRole()
    {
        return newRole;
    }
    
    /**
     * Returns the previous role the member that this event is about had.
     *
     * @return the previous role the member that this event is about had
     */
    public ChatRoomMemberRole getPreviousRole()
    {
        return previousRole;
    }
    
    /**
     * Returns the chat room that produced this event.
     *
     * @return the <tt>ChatRoom</tt> that produced this event
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom)getSource();
    }
    
    /**
     * Returns the member that this event is about.
     * @return the <tt>ChatRoomMember</tt> that this event is about
     */
    public ChatRoomMember getSourceMember()
    {
        return sourceMember;
    }
}
