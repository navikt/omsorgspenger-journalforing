package no.nav.omsorgspenger

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.*
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class JoarkClient(
        private val baseUrl: String,
        private val stsRestClient: StsRestClient,
        private val httpClient: HttpClient
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun oppdaterJournalpost(hendelseId: UUID, journalpostPayload: JournalpostPayload): Boolean {
        return httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/${journalpostPayload.journalpostId}") {
            header("Nav-Consumer-Token", hendelseId.toString())
            header("Authorization", "Bearer ${stsRestClient.token()}")
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

    suspend fun ferdigstillJournalpost(hendelseId: UUID, journalpostPayload: JournalpostPayload): Boolean {
        return httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/${journalpostPayload.journalpostId}") {
            header("Nav-Consumer-Token", hendelseId.toString())
            header("Authorization", "Bearer ${stsRestClient.token()}")
            contentType(ContentType.Application.Json)
            body = FerdigstillJournalpostPayload("9999")
        }
                .execute {
                    if (it.status.value !in 200..300) {
                        logger.warn("Feil fra Joark: {}", keyValue("response", it.receive<String>()))
                        false
                    } else true
                }

    }
}

data class FerdigstillJournalpostPayload(val journalfoerendeEnhet:String)

data class JournalpostPayload(
        val journalpostId: String,
        val tittel: String,
        val tema: String = "OMS",
        // val behandlingstema: String = "ab0061", // https://confluence.adeo.no/display/BOA/Behandlingstema
        val journalfoerendeEnhet: String = "9999",
        val bruker: Bruker,
        val sak: Sak
) {
    data class Bruker(
            val id: String,
            val idType: String = "FNR"
    )

    data class Sak(
            val sakstype: String = "FAGSAK",
            val fagsakId: String,
            val fagsaksystem: String = "K9"
    )

}