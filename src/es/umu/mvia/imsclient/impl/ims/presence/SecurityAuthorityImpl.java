/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.impl.ims.presence;

import es.umu.mvia.imsclient.service.protocol.*;

/**
 * The <tt>SecurityAuthorityImpl</tt> is an implementation of the
 * <tt>SecurityAuthority</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class SecurityAuthorityImpl implements SecurityAuthority {


    private ProtocolProviderService protocolProvider;

    /**
     * Creates an instance of <tt>SecurityAuthorityImpl</tt>.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> for this
     * <tt>SecurityAuthority</tt>.
     */
    public SecurityAuthorityImpl(ProtocolProviderService protocolProvider) {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Implements the <code>SecurityAuthority.obtainCredentials</code> method.
     * Creates and show an <tt>AuthenticationWindow</tt>, where user could enter
     * its password.
     */
    public UserCredentials obtainCredentials(String realm,
            UserCredentials userCredentials)
    {   
        return userCredentials;
    }
}
