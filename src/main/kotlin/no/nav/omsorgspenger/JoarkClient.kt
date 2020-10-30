package no.nav.omsorgspenger

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.hentRequiredEnv
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import no.nav.omsorgspenger.journalforing.Journalpost
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

internal class JoarkClient(
        private val env: Environment,
        accessTokenClient: AccessTokenClient,
        private val httpClient: HttpClient
) : HealthCheck {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val baseUrl = env.hentRequiredEnv("JOARK_BASE_URL")
    private val pingUrl = "$baseUrl/isReady"

    internal suspend fun oppdaterJournalpost(correlationId: String, journalpost: Journalpost): Boolean {
        val payload = journalpost.oppdatertJournalpostBody().also {
            secureLogger.info("Sendes til Dokarkiv for oppdatering av journalpost: $it")
        }

        return kotlin.runCatching {
            httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", "omsorgspenger-journalforing")
                header("Authorization", getAccessToken())
                contentType(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponseFraJoark()
    }

    internal suspend fun ferdigstillJournalpost(correlationId: String, journalpostId: String): Boolean {
        val payload = ferdigstillJournalpostBody.also {
            secureLogger.info("Sendes til Joark for ferdigstilling av journalpost: $it")
        }
        return kotlin.runCatching {
            httpClient.patch<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpostId}/ferdigstill") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", "omsorgspenger-journalforing")
                header("Authorization", getAccessToken())
                contentType(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponseFraJoark()
    }

    private suspend fun Result<HttpResponse>.håndterResponseFraJoark() = fold(
        onSuccess = { response -> when (response.status) {
            HttpStatusCode.OK -> true
            else -> response.secureLog()
        }},
        onFailure = { cause -> when (cause is ResponseException){
            true -> cause.response.secureLog()
            else -> throw cause
        }}
    )

    private suspend fun HttpResponse.secureLog() =
        secureLogger.error("HTTP ${status.value} fra Joark, response: ${String(content.toByteArray())}").let { false }

    private fun getAccessToken() = cachedAccessTokenClient.getAccessToken(
            setOf(env.hentRequiredEnv("DOKARKIV_SCOPES"))
    ).asAuthoriationHeader()

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

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")
        private val ferdigstillJournalpostBody = {
            @Language("JSON")
            val json = """
                {
                    "journalfoerendeEnhet": "9999"
                }
            """.trimIndent().trimJson()
            json.trimJson()
        }()
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
            "fagsaksystem": "OMSORGSPENGER",
            "fagsakId": "$saksnummer"
          }
        }
    """.trimIndent()
    return json.trimJson()
}