package no.nav.omsorgspenger

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.toByteArray
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import no.nav.omsorgspenger.journalforing.Journalpost
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal enum class JournalpostStatus {
    Oppdatert,
    Ferdigstilt,
    Feilet
}

internal class JoarkClient(
    env: Environment,
    accessTokenClient: AccessTokenClient,
    private val httpClient: HttpClient
) : HealthCheck {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val baseUrl = env.hentRequiredEnv("JOARK_BASE_URL")
    private val dokarkivScopes = setOf(env.hentRequiredEnv("DOKARKIV_SCOPES"))
    private val pingUrl = "$baseUrl/isReady"

    internal suspend fun oppdaterJournalpost(correlationId: String, journalpost: Journalpost): JournalpostStatus {
        val payload = journalpost.oppdatertJournalpostBody().also {
            secureLogger.info("[JournalpostId=${journalpost.journalpostId}] Sendes til Dokarkiv for oppdatering: $it")
        }

        return kotlin.runCatching {
            httpClient.put<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", ConsumerId)
                header("Authorization", getAuthorizationHeader())
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

    internal suspend fun ferdigstillJournalpost(correlationId: String, journalpostId: String): JournalpostStatus {
        val payload = ferdigstillJournalpostBody.also {
            secureLogger.info("[JournalpostId=${journalpostId}] Sendes til Dokarkiv for ferdigstilling: $it")
        }
        return kotlin.runCatching {
            httpClient.patch<HttpStatement>("$baseUrl/rest/journalpostapi/v1/journalpost/${journalpostId}/ferdigstill") {
                header("Nav-Callid", correlationId)
                header("Nav-Consumer-Id", ConsumerId)
                header("Authorization", getAuthorizationHeader())
                contentType(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponseFraJoark(
            http200Status = JournalpostStatus.Ferdigstilt,
            håndterIkkeHttp200 = { it.secureLog() }
        )
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

    private fun getAuthorizationHeader() = cachedAccessTokenClient.getAccessToken(dokarkivScopes).asAuthoriationHeader()

    override suspend fun check() =
            no.nav.helse.dusseldorf.ktor.health.Result.merge("JoarkClient", accessTokenCheck(), pingJoarkCheck())

    private suspend fun pingJoarkCheck() = kotlin.runCatching {
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

    private fun accessTokenCheck() = kotlin.runCatching {
        cachedAccessTokenClient.getAccessToken(dokarkivScopes).let {
            (SignedJWT.parse(it.token).jwtClaimsSet.getStringArrayClaim("roles")?.toList()?: emptyList()).contains("access_as_application")
        }}.fold(
            onSuccess = { when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }},
            onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")

        private const val ConsumerId = "omsorgspenger-journalforing"

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
        }
    """.trimIndent()
    return json.trimJson()
}