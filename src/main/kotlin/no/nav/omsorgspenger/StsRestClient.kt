package no.nav.omsorgspenger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.config.hentRequiredEnv
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StsRestClient(
        private val env: Environment,
        private val serviceUser: ServiceUser,
        private val httpClient: HttpClient = HttpClient()
) {

    private val logger: Logger = LoggerFactory.getLogger(StsRestClient::class.java)
    private val baseUrl = env.hentRequiredEnv("STS_URL")
    private val apiKey = env.hentRequiredEnv("STS_API_GW_KEY")
    private var cachedOidcToken: Token = runBlocking { fetchToken() }

    suspend fun token(): String {
        if (cachedOidcToken.expired) cachedOidcToken = fetchToken()
        return cachedOidcToken.access_token
    }

    private suspend fun fetchToken(): Token {
        try {
            return httpClient.get<HttpStatement>(
                    "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
            ) {
                header(HttpHeaders.Authorization, serviceUser.basicAuth)
                header("x-nav-apiKey", apiKey)
                accept(ContentType.Application.Json)
            }
                    .execute {
                        objectMapper.readValue(it.readText())
                    }
        } catch (e: ResponseException) {
            logger.error("Feil ved henting av token. Response: ${e.response.readText()}")
            throw RuntimeException("Feil ved henting av token")
        } catch (e: Exception) {
            logger.error("Uventet feil ved henting av token")
            throw RuntimeException("Uventet feil ved henting av token")
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