/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol;

/**
 * Contains methods that would allow service users to re-request authorizations
 * to add a contact to their contact list or, send them an authorization before
 * having been asked.
 *
 * @author Emil Ivov
 */
public interface OperationSetExtendedAuthorizations
    extends OperationSet
{
    /**
     * Send a positive authorization to <tt>contact</tt> thus allowing them to
     * add us to their contact list without needing to first request an
     * authorization.
     * @param contact the <tt>Contact</tt> whom we're granting authorization
     * prior to receiving a request.
     * @throws OperationFailedException if we fail sending the authorization.
     */
    public void explicitAuthorize(Contact contact)
        throws OperationFailedException;

    /**
     * Send an authorization request, requesting <tt>contact</tt> to add them
     * to our contact list?
     *
     * @param request the <tt>AuthorizationRequest</tt> that we'd like the
     * protocol provider to send to <tt>contact</tt>.
     * @param contact the <tt>Contact</tt> who we'd be asking for an
     * authorization.
     * @throws OperationFailedException if we fail sending the authorization
     * request.
     */
    public void reRequestAuthorization(AuthorizationRequest request,
                                       Contact contact)
        throws OperationFailedException;
}
