package no.nav.omsorgspenger

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.*
import io.ktor.client.statement.HttpStatement
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.hentRequiredEnv
import no.nav.omsorgspenger.journalforing.JournalpostPayload
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class JoarkClient(
        env: Environment,
        private val stsRestClient: StsRestClient,
        private val httpClient: HttpClient
) : HealthCheck {

    private val logger: Logger = LoggerFactory.getLogger(JoarkClient::class.java)
    private val baseUrl = env.hentRequiredEnv("JOARK_BASE_URL")
    private val apiKey = env.hentRequiredEnv("JOARK_API_GW_KEY")
    private val pingUrl = "$baseUrl/isReady"

    internal suspend fun oppdaterJournalpost(correlationId: String, journalpostPayload: JournalpostPayload): Boolean {
        return httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpostPayload.journalpostId}") {
            header("Nav-Consumer-Token", correlationId)
            header("Authorization", "Bearer ${stsRestClient.token()}")
            header("x-nav-apiKey", apiKey)
            contentType(ContentType.Application.Json)
            body = journalpostPayload
        }
                .execute {
                    if (it.status.value !in 200..300) {
                        logger.warn("Feil fra Joark: {}", keyValue("response", it.receive<String>()))
                        false
                    } else true
                }

    }

    internal suspend fun ferdigstillJournalpost(correlationId: String, journalpostPayload: JournalpostPayload): Boolean {
        return httpClient.patch<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpostPayload.journalpostId}/ferdigstill") {
            header("Nav-Consumer-Token", correlationId)
            header("Authorization", "Bearer ${stsRestClient.token()}")
            header("x-nav-apiKey", apiKey)
            contentType(ContentType.Application.Json)
            body = journalfoerendeEnhet("9999")
        }
                .execute {
                    if (it.status.value !in 200..300) {
                        logger.warn("Feil fra Joark: {}", keyValue("response", it.receive<String>()))
                        false
                    } else true
                }

    }

    internal data class journalfoerendeEnhet(val journalfoerendeEnhet: String)

    override suspend fun check() = kotlin.runCatching {
        httpClient.get<HttpStatement>(pingUrl).execute().status
    }.fold(
        onSuccess = { statusCode ->
            when (HttpStatusCode.OK == statusCode) {
                true -> Healthy("JoarkClient", "OK")
                false -> UnHealthy("JoarkClient", "Feil: Mottok Http Status Code ${statusCode.value}")
            }
        },
        onFailure = {
            UnHealthy("JoarkClient", "Feil: ${it.message}")
        }
    )
}