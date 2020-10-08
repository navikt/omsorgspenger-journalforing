package no.nav.omsorgspenger

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.put
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.omsorgspenger.journalforing.JournalpostPayload
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JoarkClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient,
        private val httpClient: HttpClient
) {

    private val logger: Logger = LoggerFactory.getLogger(JoarkClient::class.java)
    private val apiKey = System.getenv("JOARK_API_GW_KEY")?: "Test"

    suspend fun oppdaterJournalpost(correlationId: String, journalpostPayload: JournalpostPayload): Boolean {
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

    suspend fun ferdigstillJournalpost(correlationId: String, journalpostPayload: JournalpostPayload): Boolean {
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
}