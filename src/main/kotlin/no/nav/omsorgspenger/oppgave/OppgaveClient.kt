package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.AzureAwareClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.OppgaveId
import no.nav.omsorgspenger.OppgaveId.Companion.somOppgaveId
import org.slf4j.LoggerFactory
import java.net.URI

internal class OppgaveClient(
    private val baseUrl: URI,
    scopes: Set<String>,
    accessTokenClient: AccessTokenClient,
    private val httpClient: HttpClient) : AzureAwareClient(
        navn = "OppgaveClient",
        accessTokenClient = accessTokenClient,
        scopes = scopes,
        pingUrl = URI("$baseUrl/internal/ready")) {

    internal suspend fun hentJournalføringsoppgaver(
        correlationId: CorrelationId,
        aktørId: AktørId,
        journalpostIder: Set<JournalpostId>): Map<JournalpostId, OppgaveId> {
        val journalpostId = journalpostIder.joinToString { "$it" }.replace(" ", "")
        val oppgaveParams = "tema=OMS&aktoerId=$aktørId&journalpostId=$journalpostId&limit=20"
        return kotlin.runCatching {
            httpClient.prepareGet("$baseUrl/api/v1/oppgaver?$oppgaveParams") {
                header(HttpHeaders.Authorization, authorizationHeader())
                header(HttpHeaders.XCorrelationId, "$correlationId")
                accept(ContentType.Application.Json)
            }.execute()
        }.håndterResponse()
    }

    internal suspend fun opprettJournalføringsoppgave(
        correlationId: CorrelationId,
        oppgave: Oppgave): OppgaveId {
        val payload = oppgave.oppdatertOppgaveBody()
        return kotlin.runCatching {
            httpClient.preparePost("$baseUrl/api/v1/oppgaver") {
                header(HttpHeaders.Authorization, authorizationHeader())
                header(HttpHeaders.XCorrelationId, "$correlationId")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(payload)
            }.execute()
        }.håndterResponse().getValue(oppgave.journalpostId)
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): Map<JournalpostId, OppgaveId> = fold(
            onSuccess = { response ->
                when (response.status) {
                    HttpStatusCode.OK -> { // Håndter HentOppgave
                        val jsonResponse = objectMapper.readValue<JsonNode>(response.bodyAsText().toByteArray())
                        if (jsonResponse["antallTreffTotalt"].asInt() == 0) {
                            logger.info("Fann inga oppgaver")
                            return emptyMap()
                        }

                        return jsonResponse["oppgaver"].elements().asSequence().toList().associate {
                            val oppgaveid = it["id"].asText()
                            logger.info("Hentet existerande oppgave $oppgaveid")
                            val journalpostId = it["journalpostId"].asText()
                            journalpostId.somJournalpostId() to oppgaveid.somOppgaveId()
                        }
                    }
                    HttpStatusCode.Created -> { // Håndter OpprettOppgave
                        val oppgaveResponse = objectMapper.readValue<OppgaveRespons>(response.bodyAsText().toByteArray())

                        if (oppgaveResponse.id.isEmpty()) {
                            throw IllegalStateException("Uventet feil vid parsing av svar fra oppgave api, id er null")
                        }
                        logger.info("Opprettet oppgave ${oppgaveResponse.id}")
                        return mapOf(oppgaveResponse.journalpostId.somJournalpostId() to oppgaveResponse.id.somOppgaveId())
                    }
                    else -> {
                        response.logError()
                        throw IllegalStateException("Uventet response code (${response.status}) fra oppgave-api")
                    }
                }
            },
            onFailure = { cause ->
                when (cause is ResponseException) {
                    true -> {
                        cause.response.logError()
                        throw IllegalStateException("Uventet feil ved kall till oppgave-api")
                    }
                    else -> throw cause
                }
            }
    )

    private suspend fun HttpResponse.logError() =
            logger.error("HTTP ${status.value} fra oppgave-api, response: ${String(bodyAsText().toByteArray())}")

    private companion object {
        private val logger = LoggerFactory.getLogger(Oppgave::class.java)

        private val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }
}