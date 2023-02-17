package no.nav.omsorgspenger.joark

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPatch
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPut
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgspenger.AzureAwareClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import java.net.URI

internal class DokarkivClient(
    accessTokenClient: AccessTokenClient,
    private val baseUrl: URI,
    scopes: Set<String>
) : AzureAwareClient(
    navn = "DokarkivClient",
    accessTokenClient = accessTokenClient,
    scopes = scopes
) {

    private val opprettJournalpostUrl = "$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
    private fun knyttTilAnnenSakUrl(journalpostId: JournalpostId) =
        "$baseUrl/rest/journalpostapi/v1/journalpost/$journalpostId/knyttTilAnnenSak"

    private fun JournalpostId.oppdaterJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/${this}"
    private fun JournalpostId.ferdigstillJournalpostUrl() =
        "$baseUrl/rest/journalpostapi/v1/journalpost/${this}/ferdigstill"

    internal suspend fun opprettJournalpost(
        correlationId: CorrelationId,
        nyJournalpost: NyJournalpost
    ): JournalpostId {
        val (httpStatus, responseBody) = opprettJournalpostUrl.httpPost { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(nyJournalpost.dokarkivPayload())
        }.readTextOrThrow()

        return when (httpStatus == HttpStatusCode.Created || httpStatus == HttpStatusCode.Conflict) {
            true -> JSONObject(responseBody).let { json ->
                val journalpostId = json.getString("journalpostId").somJournalpostId()
                check(json.getBoolean("journalpostferdigstilt")) {
                    "Journalposten $journalpostId er ikke ferdigstilt"
                }
                journalpostId
            }

            false -> throw IllegalStateException("Feil ved opprettelse av journalpost. HttpStatus=[${httpStatus.value}], Response=[$responseBody]")
        }
    }

    internal suspend fun oppdaterJournalpostForFerdigstilling(
        correlationId: CorrelationId,
        ferdigstillJournalpost: FerdigstillJournalpost
    ) {
        val url = ferdigstillJournalpost.journalpostId.oppdaterJournalpostUrl()
        val (httpStatus, responseBody) = url.httpPut { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(ferdigstillJournalpost.oppdaterPayload())
        }.readTextOrThrow()

        check(httpStatus.isSuccess()) {
            "Feil ved oppdatering av journalpost. HttpStatus=[${httpStatus.value}, Response=[$responseBody], Url=[$url]"
        }
    }

    internal suspend fun ferdigstillJournalpost(
        correlationId: CorrelationId,
        ferdigstillJournalpost: FerdigstillJournalpost
    ) {
        val url = ferdigstillJournalpost.journalpostId.ferdigstillJournalpostUrl()
        val (httpStatus, responseBody) = url.httpPatch { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(ferdigstillJournalpost.ferdigstillPayload())
        }.readTextOrThrow()

        check(httpStatus.isSuccess()) {
            "Feil ved ferdigstilling av journalpost. HttpStatus=[${httpStatus.value}, Response=[$responseBody], Url=[$url]"
        }
    }

    internal suspend fun knyttTilAnnenSak(
        journalpostId: JournalpostId,
        fagsystem: Fagsystem,
        identitetsnummer: Identitetsnummer,
        saksnummer: Saksnummer,
        correlationId: CorrelationId
    ): JournalpostId {
        @Language("JSON")
        val dto = """
        {
            "sakstype": "FAGSAK",
            "fagsaksystem": "${fagsystem.name}",
            "fagsakId": "$saksnummer",
            "journalfoerendeEnhet": "9999",
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "$identitetsnummer"
            }
        }
        """.trimIndent()

        val url = knyttTilAnnenSakUrl(journalpostId = journalpostId)
        val (httpStatusCode, response) = url.httpPut { builder ->
            builder.header("Nav-CallId", "$correlationId")
            builder.header("Nav-Consumer-Id", "omsorgspenger-journalforing")
            builder.accept(ContentType.Application.Json)
            builder.header(HttpHeaders.Authorization, authorizationHeader())
            builder.jsonBody(dto)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra Dokarkivproxy. URL=[$url], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return JSONObject(response).getString("nyJournalpostId").somJournalpostId()
    }

    private fun HttpRequestBuilder.defaultHeaders(correlationId: CorrelationId) {
        header("Nav-Callid", "$correlationId")
        header("Nav-Consumer-Id", "omsorgspenger-journalforing")
        header("Authorization", authorizationHeader())
    }
}