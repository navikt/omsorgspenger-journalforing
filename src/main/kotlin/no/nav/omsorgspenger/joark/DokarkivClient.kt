package no.nav.omsorgspenger.joark

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.*
import io.ktor.util.toByteArray
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPatch
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPut
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.AzureAwareClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI

internal enum class JournalpostStatus {
    Oppdatert,
    Ferdigstilt,
    Feilet
}

internal class DokarkivClient(
    env: Environment,
    accessTokenClient: AccessTokenClient,
    private val httpClient: HttpClient) : AzureAwareClient(
        navn = "DokarkivClient",
        accessTokenClient = accessTokenClient,
        scopes = env.hentRequiredEnv("DOKARKIV_SCOPES").csvTilSet(),
        pingUrl = URI("${env.hentRequiredEnv("DOKARKIV_BASE_URL")}/isReady")) {

    private val baseUrl = env.hentRequiredEnv("DOKARKIV_BASE_URL")
    private val opprettJournalpostUrl = "$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
    private fun JournalpostId.oppdaterJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/${this}"
    private fun JournalpostId.ferdigstillJournalpostUrl() = "$baseUrl/rest/journalpostapi/v1/journalpost/${this}/ferdigstill"

    internal suspend fun oppdaterJournalpost(correlationId: String, journalpost: Journalpost) : JournalpostStatus {
        val payload = journalpost.oppdatertJournalpostBody().also {
            secureLogger.info("[JournalpostId=${journalpost.journalpostId}] Sendes til Dokarkiv for oppdatering: $it")
        }

        return kotlin.runCatching {
            httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", "omsorgspenger-journalforing")
                header("Authorization", authorizationHeader())
                contentType(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponseFraJoark(
            http200Status = JournalpostStatus.Oppdatert,
            håndterIkkeHttp200 = { when (it.alleredeFerdigsstilt()) {
                true -> JournalpostStatus.Ferdigstilt
                false -> it.secureLog()
            }}
        )
    }

    internal suspend fun ferdigstillJournalpost(correlationId: String, journalpostId: String) : JournalpostStatus {
        val payload = ferdigstillJournalpostBody.also {
            secureLogger.info("[JournalpostId=${journalpostId}] Sendes til Dokarkiv for ferdigstilling: $it")
        }
        return kotlin.runCatching {
            httpClient.patch<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpostId}/ferdigstill") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", "omsorgspenger-journalforing")
                header("Authorization", authorizationHeader())
                contentType(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponseFraJoark(
            http200Status = JournalpostStatus.Ferdigstilt,
            håndterIkkeHttp200 = { it.secureLog() }
        )
    }

    internal suspend fun opprettJournalpost(
        correlationId: CorrelationId,
        nyJournalpost: NyJournalpost
    ): JournalpostId {
        val (httpStatus, responseBody) = opprettJournalpostUrl.httpPost { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(nyJournalpost.dokarkivPayload())
        }.readTextOrThrow()

        return when (httpStatus == HttpStatusCode.Created || httpStatus == HttpStatusCode.Conflict)  {
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
        ferdigstillJournalpost: FerdigstillJournalpost) {
        val url = ferdigstillJournalpost.journalpostId.oppdaterJournalpostUrl()
        val (httpStatus, responseBody) = url.httpPut { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(ferdigstillJournalpost.oppdaterPayload())
        }.readTextOrThrow()

        check(httpStatus.isSuccess()) {
            "Feil ved oppdatering av journalpost. HttpStatus=[${httpStatus.value}, Response=[$responseBody], Url=[$url]"
        }
    }

    internal suspend fun ferdigstillJournalposten(
        correlationId: CorrelationId,
        ferdigstillJournalpost: FerdigstillJournalpost) {
        val url = ferdigstillJournalpost.journalpostId.ferdigstillJournalpostUrl()
        val (httpStatus, responseBody) = url.httpPatch { builder ->
            builder.defaultHeaders(correlationId)
            builder.jsonBody(ferdigstillJournalpost.ferdigstillPayload())
        }.readTextOrThrow()

        check(httpStatus.isSuccess()) {
            "Feil ved ferdigstilling av journalpost. HttpStatus=[${httpStatus.value}, Response=[$responseBody], Url=[$url]"
        }
    }

    private fun HttpRequestBuilder.defaultHeaders(correlationId: CorrelationId) {
        header("Nav-Callid", "$correlationId")
        header("Nav-Consumer-Id", "omsorgspenger-journalforing")
        header("Authorization", authorizationHeader())
    }

    private suspend fun Result<HttpResponse>.håndterResponseFraJoark(
        http200Status: JournalpostStatus,
        håndterIkkeHttp200: (pair: Pair<HttpStatusCode, String>) -> JournalpostStatus
    ) = fold(
        onSuccess = { response -> when (response.status) {
            HttpStatusCode.OK -> http200Status
            else -> håndterIkkeHttp200(response.toPair())
        }},
        onFailure = { cause -> when (cause is ResponseException){
            true -> cause.response.toPair().secureLog()
            else -> throw cause
        }}
    )

    private suspend fun HttpResponse.toPair() = status to String(content.toByteArray())
    private fun Pair<HttpStatusCode, String>.secureLog() =
        secureLogger.error("HTTP ${first.value} fra Dokarkiv, response: $second").let { JournalpostStatus.Feilet }

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")

        private fun String.json() = kotlin.runCatching { JSONObject(this) }.fold(
            onSuccess = { it },
            onFailure = { JSONObject() }
        )

        private fun Pair<HttpStatusCode, String>.alleredeFerdigsstilt() = when {
            first == HttpStatusCode.BadRequest -> {
                second.json().let {
                    it.has("message") && it.getString("message").contains("journalpostStatus=J")
                }
            }
            else -> false
        }

        private val ferdigstillJournalpostBody = run {
            @Language("JSON")
            val json = """
                    {
                        "journalfoerendeEnhet": "9999"
                    }
                """.trimIndent()
            json.trimJson()
        }
    }
}

private fun Journalpost.oppdatertJournalpostBody() : String {
    @Language("JSON")
    val json = """
        {
          "tema": "OMS",
          "bruker": {
            "idType": "FNR",
            "id": "$identitetsnummer"
          },
          "sak": {
            "sakstype": "FAGSAK",
            "fagsaksystem": "${fagsaksystem.name}",
            "fagsakId": "$saksnummer"
          }
          ${if(navn != null) {
              ""","avsenderMottaker": {"navn": "$navn"}"""
          } else ""}
        }
    """.trimIndent()
    return json.trimJson()
}