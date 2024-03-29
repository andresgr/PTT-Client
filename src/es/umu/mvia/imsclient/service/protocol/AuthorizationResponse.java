/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package es.umu.mvia.imsclient.service.protocol;

/**
 * This class is used to represent both incoming and outgoing
 * AuthorizationResponse-s
 * <p>
 * An outgoing Authorization Response is to be created by the user interface
 * when an authorization request has been received from the network. The user
 * inteface or any other bundle responsible of handling such responses is to
 * implement the AuthoizationHandler interface and register itself with a
 * protocol provider. Whenever a resonse needs to be sent the protocol provider
 * would ask the the AuthorizationHandler to create one through the
 * processAuthorizationRequest() method.
 * <p>
 * Incomfing Authorization responses are delivered to the AuthorizationHandler
 * implementation through the AuthorizationHandler.processAuthorizationResponse()
 * method.
 *
 * @author Emil Ivov
 */
public class AuthorizationResponse
{
    /**
     * Indicates that the source authorization request which this response is
     * about has been accepted and that the requestor may now proceed to adding
     * the user to their contact list.
     */
    public static final AuthorizationResponseCode ACCEPT
        = new AuthorizationResponseCode("Accept");

    /**
     * Indicates that source authorization request which this response is about
     * has been rejected. A reason may also have been specified.
     */
    public static final AuthorizationResponseCode REJECT
        = new AuthorizationResponseCode("Reject");

    /**
     * Indicates that source authorization request which this response is about
     * has been ignored and that no other indication will be sent to the
     * requestor.
     */
    public static final AuthorizationResponseCode IGNORE
        = new AuthorizationResponseCode("Ignore");

    /**
     * A reason indication that the source user may or may not add to explaing
     * the resposne.
     */
    private String reason = null;

    /**
     * Authorization response codes represent unambiguous indication of the way
     * a user or a remote party have acted upon an authorization request.
     */
    private AuthorizationResponseCode responseCode = null;

    /**
     * Creates an instance of an AuthorizationResponse with the specified
     * responseCode and reason.
     * @param responseCode AuthorizationResponseCode
     * @param reason String
     */
    public AuthorizationResponse(AuthorizationResponseCode responseCode,
            String reason)
    {
        this.reason = reason;
        this.responseCode = responseCode;
    }

    /**
     * Returns a String containing additional explanations (might also be empty)
     * of this response.
     * @return the reason the source user has given to explain this resonse and
     * null or an empty string if no reason has been specified.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns the response code that unambiguously represents the sense of this
     * respone.
     * @return an AuthorizationResponseResponseCode instance determining the
     * nature of the response.
     */
    public AuthorizationResponseCode getResponseCode()
    {
        return responseCode;
    }


    /**
     * Authorization response codes represent unambiguous indication of the way
     * a user or a remote party have acted upon an authorization request.
     */
    public static class AuthorizationResponseCode
    {
        private String code = null;

        private AuthorizationResponseCode(String code)
        {
            this.code = code;
        }

        /**
         * Returns the string contents representing this code.
         * @return a String representing the code.
         */
        public String getCode()
        {
            return code;
        }

    }
}
