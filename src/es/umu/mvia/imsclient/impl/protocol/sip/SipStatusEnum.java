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
import java.io.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a SIP contact can currently have.
 *
 * @author Emil Ivov
 */
public class SipStatusEnum
    extends PresenceStatus
{
    private static final Logger logger
        = Logger.getLogger(SipStatusEnum.class);


    /**
     * Initialize the list of supported status states.
     */
    public static Map<String,SipStatusEnum> supportedStatusSet = new HashMap<String,SipStatusEnum>();

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final SipStatusEnum OFFLINE
        = new SipStatusEnum(0, "Offline", "Offline");

    /**
     * The busy status. Indicates that the user has connectivity but is doing
     * something else.
     */
    public static final SipStatusEnum BUSY
        = new SipStatusEnum(30,"Busy", "Busy");
    
    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    public static final SipStatusEnum ON_THE_PHONE
        = new SipStatusEnum(37,"Phone", "On the phone");
    
    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final SipStatusEnum AWAY
        = new SipStatusEnum(40,"Away", "Away");

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final SipStatusEnum ONLINE
        = new SipStatusEnum(65, "Online", "Online");
    
    /**
     * The Unknown status. Indicate that we don't know if the user is present
     * or not.
     */
    public static final SipStatusEnum UNKNOWN = new SipStatusEnum(1,"Unknown", "Unknown");

    /*static
    {
        supportedStatusSet.add(ONLINE);
        supportedStatusSet.add(AWAY);
        supportedStatusSet.add(ON_THE_PHONE);
        supportedStatusSet.add(BUSY);
        supportedStatusSet.add(OFFLINE);
    }*/

    /**
     * Creates an instance of <tt>SipPresenceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusDescr the description of the presence status.
     */
    private SipStatusEnum(int status,
                                String statusName, String statusDescr)
    {
        super(status, statusName.trim(), statusDescr);
        supportedStatusSet.put(statusName, this);
    }


    /**
     * Creates an instance of <tt>SipPresenceStatus</tt> with the
     * specified parameters.
     * @param statusName the name of the presence status.
     * @param statusDescr the description of the presence status.
     */
    public SipStatusEnum(String statusName, String statusDescr) {
        /*if (statusDescr == null)
            statusDescr = statusName;*/
        this(50, statusName, statusDescr != null ? statusDescr : statusName);
    }


    /**
     * Creates an instance of <tt>SipPresenceStatus</tt> with the
     * specified parameters.
     * @param statusName the name of the presence status.
     */
    public SipStatusEnum(String statusName) {
        this(50, statusName, statusName);
    }


    /**
     * Returns an iterator over all status instances supproted by the sip
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * sip provider.
     */
    static Iterator<SipStatusEnum> supportedStatusSet()
    {
        return supportedStatusSet.values().iterator();
    }


    static SipStatusEnum getStatus (String status) {
        status = status.trim();
        /*Iterator it = supportedStatusSet.keySet().iterator();
        while (it.hasNext()){
            String clave = (String)it.next();
            logger.debug("Clave: " + clave);
        }*/
        return supportedStatusSet.get(status);
    }


    /**
     * Loads an image from a given image path.
     * @param imagePath The path to the image resource.
     * @return The image extracted from the resource at the specified path.
     */
    public static byte[] loadIcon(String imagePath)
    {
        InputStream is = SipStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);

        byte[] icon = null;
        try
        {
            icon = new byte[is.available()];
            is.read(icon);
        }
        catch (IOException exc)
        {
            logger.error("Failed to load icon: " + imagePath, exc);
        }
        return icon;
    }

}
