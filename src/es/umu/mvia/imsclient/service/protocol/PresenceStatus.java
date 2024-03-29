/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol;

/**
 * The class is used to represent the state of the connection
 * of a given ProtocolProvider or Contact. It is up to the implementation to
 * determine the exact states that an object might go through. An IM provider
 * for example might go through states like, CONNECTING, ON-LINE, AWAY, etc, A
 * status instance is represented by an integer varying from 0 to
 * 100, a Status Name and a Status Description.
 *
 * The integer status variable is used so that the users of the service get the
 * notion of whether or not a given Status instance represents a state that
 * allows communication (above 20) and so that it could compare instances
 * between themselves (e.g. for sorting a ContactList for example).
 *
 * A state may not be created by the user. User may request a status change
 * giving parameters requested by the ProtocolProvider. Once a statue is
 * successfully entered by the provider, a ConnectivityStatus instacne is
 * conveyed to the user through a notification event.
 *
 * @author Emil Ivov
 */
public class PresenceStatus
        implements Comparable
{
    /**
     * An integer above which all values of the status coefficient indicate
     * that a status with connectivity (communication is possible).
     */
    public static final int ONLINE_THRESHOLD = 20;

    /**
     * An integer above which all values of the status coefficient indicate
     * both connectivity and availability.
     */
    public static final int AVAILABLE_THRESHOLD = 50;

    /**
     * An integer above which all values of the status coefficient indicate
     * eagerness to communicate
     */
    public static final int EAGER_TO_COMMUNICATE_THRESSHOLD = 80;

    /**
     * An integer indicating the maximum possible value of the status field.
     */
    public static final int MAX_STATUS_VALUE = 100;

    /**
     * An image that graphically represents the status.
     */
    protected byte[] statusIcon = null;

    /**
     * Represents the connectivity status on a scale from
     * 0 to 100  with 0 indicating complete disabiilty for communication and 100
     * maximum ability and user willingness. Implementors of this service should
     * respect the following indications for status values.
     * 0      - complete disability
     * 1:10   - initializing.
     * 1:20   - trying to enter a state where communication is possible
     *          (Connecting ..)
     * 20:50  - communication is possible but might be unwanted, inefficient or
     *          delayed(e.g. Away state in IM clients)
     * 50:80  - communication is possible (On - line)
     * 80:100 - communication is possible and user is eager to communicate.
     *        (Free for chat! Talk to me, etc.)
     */
    protected int status = 0;

    /**
     * The name of this status instance (e.g. Away, On-line, Invisible, etc.)
     */
    protected String statusName = null;

    /**
     * The description of this status instance
     */
    protected String statusDescription = null;



    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     * @param statusDescr the description of this PresenceStatus
     */
    protected PresenceStatus(int status, String statusName, String statusDescr)
    {
        this(status, statusName, statusDescr, null);
    }

    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     */
    protected PresenceStatus(int status, String statusName)
    {
        this(status, statusName, null, null);
    }
    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     * @param statusDescr the description of this PresenceStatus
     * @param statusIcon an image that graphically represents the status or null
     * if no such image is available.
     */
    protected PresenceStatus(int status, String statusName, String statusDescr, byte[] statusIcon)
    {
        this.status = status;
        this.statusName = statusName;
        this.statusDescription = statusDescr;
        this.statusIcon = statusIcon;
    }

    
    /**
     * Returns an integer representing the presence status on a scale from
     * 0 to 100.
     * @return a short indicating the level of availability corresponding to
     * this status object.
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Returns the name of this status (such as Away, On-line, Invisible, etc).
     * @return a String variable containing the name of this status instance.
     */
    public String getStatusName()
    {
        return statusName;
    }

    /**
     * Returns the description of this status.
     * @return a String variable containing the description of this status instance.
     */
    public String getStatusDescription() {
        return statusDescription;
    }


    /**
     * Sets the description of this status.
     * @param descr The description of the state
     */
    public void setStatusDescription(String descr) {
        statusDescription = descr;
    }



    /**
     * Returns a string represenation of this provider status. Strings returned
     * by this method have the following format: PresenceStatus:<STATUS_STRING>:
     * <STATUS_MESSAGE> and are meant to be used for loggin/debugging purposes.
     * @return a string representation of this object.
     */
    public String toString()
    {
        return "PresenceStatus:" + getStatusName();
    }

    /**
     * Indicates whether the user is Online (can be reached) or not.
     * @return true if the the status coefficient is higher than the
     * ONLINE_THRESHOLD and false otherwise
     */
    public boolean isOnline()
    {
        return getStatus() >= ONLINE_THRESHOLD;
    }

    /**
     * Indicates whether the user is both Online and avaliable (can be reached
     * and is likely to respond) or not.
     * @return true if the the status coefficient is higher than the
     * AVAILABLE_THRESHOLD and false otherwise
     */
    public boolean isAvailable()
    {
        return getStatus() >= AVAILABLE_THRESHOLD;
    }

    /**
     * Indicates whether the user is Online, available and eager to communicatie
     * (can be reached and is likely to become annoyngly talkative if contacted).
     *
     * @return true if the the status coefficient is higher than the
     * EAGER_TO_COMMUNICATE_THRESHOLD and false otherwise
     */
    public boolean isEagerToCommunicate()
    {
        return getStatus() >= EAGER_TO_COMMUNICATE_THRESSHOLD;
    }

    /**
     * Compares this inatance with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this status instance is
     * considered to represent less, as much, or more availabilite than the one
     * specified by the parameter.<p>
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *            is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     * @throws NullPointerException if o is null
     */
    public int compareTo(Object o)
        throws ClassCastException, NullPointerException
    {
        PresenceStatus target = (PresenceStatus)o;
        return (getStatus() - target.getStatus());
    }

    /**
     * Indicates whether some other object is "equal to" this one. To
     * PresenceStatus instances are considered equal if and only if both their
     * connecfitivity coefficient and their name are equal.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this presence status instance is equal to
     *          the obj argument; <tt>false</tt> otherwise.
     */
    public boolean equals(Object obj)
    {
        if (obj == null
            || !(obj instanceof PresenceStatus) )
        return false;

        PresenceStatus status = (PresenceStatus)obj;

        if (status.getStatus() != getStatus()
            || !status.getStatusName().equals(statusName))
            return false;

        return true;
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     *
     * @return  a hash code value for this object (which is actually the result
     * of the getStatusName().hashCode()).
     */
    public int hashCode()
    {
        return getStatusName().hashCode();
    }

    /**
     * Returns an image that graphically represents the status.
     *
     * @return a byte array containing the image that graphically represents
     * the status or null if no such image is available.
     */
    public byte[] getStatusIcon()
    {
        return statusIcon;
    }
}
