package no.nav.sf.audit.logging.token

import org.http4k.client.ApacheClient.invoke
import org.http4k.core.Request.Companion.invoke

/**
 * A handler for oauth2 access flow to salesforce.
 * @see [sf.remoteaccess_oauth_jwt_flow](https://help.salesforce.com/s/articleView?id=sf.remoteaccess_oauth_jwt_flow.htm&type=5)
 *
 * Fetches and caches access token, also retrieves instance url
 */
class DefaultAccessTokenHandler : AccessTokenHandler
