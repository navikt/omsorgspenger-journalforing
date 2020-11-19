package no.nav.omsorgspenger

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import no.nav.omsorgspenger.oppgave.Oppgave
import no.nav.omsorgspenger.oppgave.OppgaveRespons
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class OppgaveClient(
        private val env: Environment,
        accessTokenClient: AccessTokenClient,
        private val httpClient: HttpClient
) : HealthCheck {
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val baseUrl = env.hentRequiredEnv("OPPGAVE_BASE_URL")
    private val oppgaveScopes = setOf(env.hentRequiredEnv("OPPGAVE_SCOPES"))
    private val pingUrl = "$baseUrl/isReady"

    internal suspend fun opprettOppgave(correlationId: String, oppgave: Oppgave): OppgaveRespons {
        val payload = oppgave.oppdatertOppgaveBody().also {
            secureLogger.info("[CorrelationId: $correlationId] Sendes til OppgaveApi for oppretting av oppgave")
        }

        return httpClient.post<HttpStatement>("$baseUrl/api/v1/oppgaver") {
            header("Authorization", getAuthorizationHeader())
            header("X-Correlation-ID", correlationId)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = payload
        }.receive()
    }

    private fun getAuthorizationHeader() = cachedAccessTokenClient.getAccessToken(oppgaveScopes).asAuthoriationHeader()

    override suspend fun check() =
            no.nav.helse.dusseldorf.ktor.health.Result.merge("OppgaveClient", accessTokenCheck(), pingOppgaveApiCheck())

    private suspend fun pingOppgaveApiCheck() = kotlin.runCatching {
        httpClient.get<HttpStatement>(pingUrl).execute().status
    }.fold(
            onSuccess = { statusCode ->
                when (HttpStatusCode.OK == statusCode) {
                    true -> Healthy("OppgaveApi", "OK")
                    false -> UnHealthy("OppgaveApi", "Feil: Mottok Http Status Code ${statusCode.value}")
                }
            },
            onFailure = {
                UnHealthy("OppgaveApi", "Feil: ${it.message}")
            }
    )

    private fun accessTokenCheck() = kotlin.runCatching {
        cachedAccessTokenClient.getAccessToken(oppgaveScopes).let {
            (SignedJWT.parse(it.token).jwtClaimsSet.getStringArrayClaim("roles")?.toList()
                    ?: emptyList()).contains("access_as_application")
        }
    }.fold(
            onSuccess = {
                when (it) {
                    true -> Healthy("AccessTokenCheck", "OK")
                    false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
                }
            },
            onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )

    private companion object {
        private val secureLogger = LoggerFactory.getLogger("tjenestekall")

        private fun String.json() = kotlin.runCatching { JSONObject(this) }.fold(
                onSuccess = { it },
                onFailure = { JSONObject() }
        )
    }
}

private fun Oppgave.oppdatertOppgaveBody(): String {
    @Language("JSON")
    val json = """
        {
          "tema": "OMS",
          "journalpostId": "$journalpostId",
          "journalpostType": "$journalpostType",
          "prioritet": "NORM",
          "aktivDato": "yyyy-mm-dd",
          "oppgavetype": "",
          "aktoerId": "$aktoerId"
        }
    """.trimIndent()
    //TODO("Vad ær oppgavetype?)
    return json.trimJson()
}