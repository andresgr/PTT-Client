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
 * <tt>WhiteboardObjectModifiedEvent</tt>s indicate that a WhiteboardObject
 * has been modified remotely.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardObjectModifiedEvent
  extends EventObject
{
    /**
     * The contact that has sent this wbObject.
     */
    private Contact from = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * A reference to the whiteboard object that has been modified.
     */
    private WhiteboardObject obj;

    /**
     * Creates a <tt>WhiteboardObjectModifiedEvent</tt> representing
     * reception of the modified <tt>source</tt> WhiteboardObject
     * received from the specified <tt>from</tt> contact.
     *
     * @param source the <tt>WhiteboardSession</tt>
     * @param obj the <tt>WhiteboardObject</tt> whose reception
     * this event represents.
     * @param from the <tt>Contact</tt> that has sent this WhiteboardObject.
     * @param timestamp the exact date when the event ocurred.
     */
    public WhiteboardObjectModifiedEvent (WhiteboardSession source,
      WhiteboardObject obj, Contact from, Date timestamp)
    {
        super (source);
        this.obj = obj;
        this.from = from;
        this.timestamp = timestamp;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has send the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>Contact</tt> that has send the
     * <tt>WhiteboardObject</tt> whose reception this event represents.
     */
    public Contact getSourceContact ()
    {
        return from;
    }

    /**
     * Returns the WhiteboardObject that triggered this event
     *
     * @return the <tt>WhiteboardObject</tt> that triggered this event.
     */
    public WhiteboardObject getSourceWhiteboardObject ()
    {
        return obj;
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     *
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp ()
    {
        return timestamp;
    }
}
