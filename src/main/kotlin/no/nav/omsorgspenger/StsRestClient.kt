package no.nav.omsorgspenger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class StsRestClient(
        private val baseUrl: String,
        private val serviceUser: ServiceUser,
        private val httpClient: HttpClient = HttpClient()
) {

    private val logger: Logger = LoggerFactory.getLogger(StsRestClient::class.java)
    private var cachedOidcToken: Token = runBlocking { fetchToken() }
    private val apiKey = System.getenv("STS_API_GW_KEY")

    suspend fun token(): String {
        if (cachedOidcToken.expired) cachedOidcToken = fetchToken()
        return cachedOidcToken.access_token
    }

    private suspend fun fetchToken(): Token {
        try {
            return httpClient.get<HttpStatement>(
                    "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
            ) {
                header("Authorization", serviceUser.basicAuth)
                header("x-nav-apiKey", apiKey)
                accept(ContentType.Application.Json)
            }
                    .execute {
                        objectMapper.readValue(it.readText())
                    }
        } catch (e: ServerResponseException) {
            logger.error("Feil ved henting av token. Response: ${e.response.readText()}", e)
            throw RuntimeException("Feil ved henting av token", e)
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av token", e)
            throw RuntimeException("Uventet feil ved henting av token", e)
        }
    }

    internal data class Token(
            internal val access_token: String,
            private val token_type: String,
            private val expires_in: Long
    ) {
        // expire 10 seconds before actual expiry. for great margins.
        private val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expires_in - 10L)
        internal val expired get() = expirationTime.isBefore(LocalDateTime.now())
    }
}