/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol.event;

import java.util.*;

/**
 *
 * @author Emil Ivov
 */
public interface PresenceStatusListener
    extends EventListener
{
    public void contactPresenceStatusChanged();

}
