/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.impl.protocol.sip;

import java.util.*;

import es.umu.mvia.imsclient.service.protocol.*;

/**
 * A SIP extension of the account ID property.
 * @author Emil Ivov
 */
public class SipAccountID
    extends AccountID
{

    /**
     * Creates a SIP account id from the specified ide and account properties.
     *
     * @param userID the user id part of the SIP uri identifying this contact.
     * @param accountProperties any other properties necessary for the account.
     * @param serverName the name of the server that the user belongs to.
     */
    protected SipAccountID(String userID,
                           Map    accountProperties,
                           String serverName)
    {
        super( userID, accountProperties, ProtocolNames.SIP, serverName);
        /*super( ( userID.indexOf("@") > -1 )
                    ? userID.substring(0, userID.indexOf("@"))
                    : userID
                , accountProperties
                , ProtocolNames.SIP
                , serverName);*/
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the procotol can use to communicate with us.
     * By default this string is set to userid@servicename. Protocol
     * implementors should override it if they'd need it to respect a different
     * syntax.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        return "sip:" + getUserID() + "@" + getService();
    }

}
