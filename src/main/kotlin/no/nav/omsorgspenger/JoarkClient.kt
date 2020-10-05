package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.put
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.omsorgspenger.journalforing.BehovPayload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class JoarkClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient,
        private val httpClient: HttpClient
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun oppdaterJournalpost(hendelseId: UUID, behovPayload: BehovPayload): Boolean {
        return httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/${behovPayload.journalpostId}") {
            header("Nav-Consumer-Token", hendelseId.toString())
            header("Authorization", "Bearer ${stsRestClient.token()}")
            contentType(ContentType.Application.Json)
            body = behovPayload
        }
                .execute {
                    if (it.status.value !in 200..300) {
                        logger.warn("Feil fra Joark: {}", keyValue("response", it.receive<String>()))
                        false
                    } else true
                }

    }

    suspend fun ferdigstillJournalpost(hendelseId: UUID, behovPayload: BehovPayload): Boolean {
        return httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${behovPayload.journalpostId}/ferdigstill") {
            header("Nav-Consumer-Token", hendelseId.toString())
            header("Authorization", "Bearer ${stsRestClient.token()}")
            contentType(ContentType.Application.Json)
            body = behovPayload.journalfoerendeEnhet
        }
                .execute {
                    if (it.status.value !in 200..300) {
                        logger.warn("Feil fra Joark: {}", keyValue("response", it.receive<String>()))
                        false
                    } else true
                }

    }
}