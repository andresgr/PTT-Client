package es.umu.mvia.imsclient.impl.protocol.sip.security;

import java.util.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.address.*;
import javax.sip.message.*;

/**
 * The class is used to cache all realms that a certain call has been authorized
 * against and all credentials that have been used for each realm. Note that
 * rfc3261 suggests keeping callId->credentials mapping where as we map
 * realm->credentials. This is done to avoid asking the user for a password
 * before each call.
 *
 * @author Emil Ivov
 * @author Nie Pin
 * @version 1.0
 */

class CredentialsCache
{
    /**
     * Contains call->realms mappings
     */
    private Hashtable authenticatedRealms = new Hashtable();

    /**
     * Contains callid->authorization header mappings
     */
    private Hashtable authenticatedCalls =  new Hashtable();

    /**
     * Cache credentials for the specified call and realm
     * @param realm the realm that the specify credentials apply to
     * @param cacheEntry the credentials
     */
    void cacheEntry(String realm,
                    CredentialsCacheEntry cacheEntry)
    {
        authenticatedRealms.put(realm, cacheEntry);
    }

    /**
     * Returns the credentials corresponding to the specified realm
     * or null if none could be found.
     *
     * @param realm the realm that the credentials apply to
     * @return the credentials corresponding to the specified realm
     * or null if none could be found.
     */
    CredentialsCacheEntry get(String realm)
    {
        return (CredentialsCacheEntry)this.authenticatedRealms.get(realm);
    }

    /**
     * Returns the credentials corresponding to the specified realm
     * or null if none could be found and removes the entry from the cache.
     *
     * @param realm the realm that the credentials apply to
     * @return the credentials corresponding to the specified realm
     * or null if none could be found.
     */
    CredentialsCacheEntry remove(String realm)
    {
        return (CredentialsCacheEntry)this.authenticatedRealms.remove(realm);
    }

    /**
     * Empty the credentials cache (all authorization challenges) would end up
     * requesting a password from the user.
     */
    void clear()
    {
        authenticatedRealms.clear();
    }

    /**
     * Cache the bindings of call-id and authorization header.
     *
     * @param callid the id of the call that the <tt>authorization</tt> header
     * belongs to.
     * @param authorization the authorization header that we'd like to cache.
     */
    void cacheAuthorizationHeader(String callid,
                                  AuthorizationHeader authorization)
    {
        authenticatedCalls.put(callid, authorization);
    }

    /**
     * Returns an authorization header cached for the specified call id and null
     * if no authorization header has been previously cached for this call.
     *
     * @param callid the call id that we'd like to retrive a cached
     * authorization header for.
     *
     * @return authorization header corresponding to the specified callid
     */
    AuthorizationHeader getCachedAuthorizationHeader(String callid)
    {
        return (AuthorizationHeader)this.authenticatedCalls.get(callid);
    }

}
