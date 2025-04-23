package no.nav.sf.audit.logging.token

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sf.audit.logging.config_SF_TOKENHOST
import no.nav.sf.audit.logging.env
import no.nav.sf.audit.logging.secret_KEYSTORE_JKS_B64
import no.nav.sf.audit.logging.secret_KEYSTORE_PASSWORD
import no.nav.sf.audit.logging.secret_PRIVATE_KEY_ALIAS
import no.nav.sf.audit.logging.secret_PRIVATE_KEY_PASSWORD
import no.nav.sf.audit.logging.secret_SF_CLIENT_ID
import no.nav.sf.audit.logging.secret_SF_USERNAME
import org.apache.commons.codec.binary.Base64
import org.http4k.client.ApacheClient
import org.http4k.client.ApacheClient.invoke
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Request.Companion.invoke
import org.http4k.core.Response
import org.http4k.core.body.toBody
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey

/**
 * A handler for oauth2 access flow to salesforce.
 * @see [sf.remoteaccess_oauth_jwt_flow](https://help.salesforce.com/s/articleView?id=sf.remoteaccess_oauth_jwt_flow.htm&type=5)
 *
 * Fetches and caches access token, also retrieves instance url
 */
class DefaultAccessTokenHandler : AccessTokenHandler {
   
}
