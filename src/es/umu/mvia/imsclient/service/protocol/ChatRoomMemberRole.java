/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol;

/**
 * Indicates roles that a chat room member detains in its containing chat room.
 *
 * @author Emil Ivov
 */
public class ChatRoomMemberRole
        implements Comparable
{
    /**
     * A role implying the full set of chat room permissions
     */
    public static final ChatRoomMemberRole OWNER
                                = new ChatRoomMemberRole("Owner", 70);

    /**
     * A role implying administrative permissions.
     */
    public static final ChatRoomMemberRole ADMINISTRATOR
        = new ChatRoomMemberRole("Administrator", 60);

    /**
     * A role implying moderator permissions.
     */
    public static final ChatRoomMemberRole MODERATOR
        = new ChatRoomMemberRole("Moderator", 50);

    /**
     * A role implying standard participant permissions.
     */
    public static final ChatRoomMemberRole MEMBER
        = new ChatRoomMemberRole("Member", 40);

    /**
     * A role implying standard participant permissions.
     */
    public static final ChatRoomMemberRole GUEST
        = new ChatRoomMemberRole("Guest", 30);


    /**
     * A role implying standard participant permissions without the right to
     * send messages/speak.
     */
    public static final ChatRoomMemberRole SILENT_MEMBER
        = new ChatRoomMemberRole("SilentMember", 30);

    /**
     * A role implying an explicit ban for the user to join the room.
     */
    public static final ChatRoomMemberRole OUTCAST
        = new ChatRoomMemberRole("Outcast", 20);

    /**
     * the name of this role.
     */
    private String roleName = null;

    /**
     * The index of a role is used to allow ordering of roles by other modules
     * (like the UI) that would not necessarily "know" all possible roles.
     * Higher values of the role index indicate roles with more permissions and
     * lower values pertain to more restrictive roles.
     */
    private int roleIndex;

    /**
     * Creates a role with the specified <tt>roleName</tt>. The constructor
     * is protected in case protocol implementations need to add extra roles
     * (this should only be done when absolutely necessary in order to assert
     * smooth interoperability with the user interface).
     *
     * @param roleName the name of this role.
     * @param roleIndex an int that would allow to compare this role to others
     * according to the set of permissions that it implies.
     *
     * @throws java.lang.NullPointerException if roleName is null.
     */
    protected ChatRoomMemberRole(String roleName, int roleIndex)
        throws NullPointerException
    {
        if(roleName == null)
            throw new NullPointerException("Role Name can't be null.");

        this.roleName = roleName;
        this.roleIndex = roleIndex;
    }

    /**
     * Returns the name of this role.
     *
     * @return the name of this role.
     */
    public String getRoleName()
    {
        return this.roleName;
    }

    /**
     * Returns a localized (i18n) name role name.
     *
     * @return a i18n version of this role name.
     */
    public String getLocalizedRoleName()
    {
        return this.roleName;
    }

    /**
     * Returns a role index that can be used to allow ordering of roles by
     * other modules (like the UI) that would not necessarily "know" all
     * possible roles.  Higher values of the role index indicate roles with
     * more permissions and lower values pertain to more restrictive roles.
     *
     * @return an <tt>int</tt> that when compared to role indexes of other
     * roles can provide an ordering for the different role instances.
     */
    public int getRoleIndex()
    {
        return roleIndex;
    }

    /**
     * Indicates whether some other object is "equal to" this role instance.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if obj is a role instance that has the same
     * name and role index as this one.
     */
    public boolean equals(Object obj)
    {
        if( obj == null
            || !(obj instanceof ChatRoomMemberRole)
            || !((ChatRoomMemberRole)obj).getRoleName().equals(roleName)
            || ((ChatRoomMemberRole)obj).getRoleIndex() != getRoleIndex())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p>
     * @return  a hash code value for this object.
     */
    public int hashCode()
    {
        return getRoleName().hashCode();
    }

    /**
     * Compares this role's role index with that of the specified object for
     * order.  Returns a negative integer, zero, or a positive integer as this
     * role is less than, equal to, or greater than the specified object.
     *
     * @param   obj the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *            is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type is not an
     * instance of ChatRoomMemberRole.
     */
    public int compareTo(Object obj)
        throws ClassCastException
    {
        return new Integer(getRoleIndex())
            .compareTo(new Integer(((ChatRoomMemberRole)obj).getRoleIndex()));
    }

}
