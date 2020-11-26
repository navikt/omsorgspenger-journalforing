package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.toByteArray
import java.util.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.oppgave.Oppgave
import no.nav.omsorgspenger.oppgave.OppgaveRespons
import no.nav.omsorgspenger.oppgave.oppdatertOppgaveBody
import org.json.JSONArray
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

    internal suspend fun hentOppgave(correlationId: String, aktørId: String, journalpostIder: Set<String>): OppgaveLøsning {
        val journalpostId = journalpostIder.joinToString().replace(" ", "")
        val oppgaveParams = "tema=OMS&aktoerId=$aktørId&journalpostId=$journalpostId&limit=20"
        return kotlin.runCatching {
            httpClient.get<HttpStatement>("$baseUrl/api/v1/oppgaver?$oppgaveParams") {
                header("Authorization", getAuthorizationHeader())
                header("X-Correlation-ID", correlationId)
                accept(ContentType.Application.Json)
            }.execute()
        }.håndterResponse()
    }

    internal suspend fun opprettOppgave(correlationId: String, oppgave: Oppgave): OppgaveLøsning {
        val payload = oppgave.oppdatertOppgaveBody()
        return kotlin.runCatching {
            httpClient.post<HttpStatement>("$baseUrl/api/v1/oppgaver") {
                header("Authorization", getAuthorizationHeader())
                header("X-Correlation-ID", correlationId)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = payload
            }.execute()
        }.håndterResponse()
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): OppgaveLøsning = fold(
            onSuccess = { response ->
                when (response.status) {
                    HttpStatusCode.OK -> { // Håndter HentOppgave
                        val response = objectMapper.readValue<JsonNode>(response.content.toByteArray())
                        if (response["antallTreffTotalt"].asInt() == 0) {
                            logger.info("Fann inga oppgaver")
                            return emptyMap()
                        }

                        return response["oppgaver"].elements().asSequence().toList().map {
                            val oppgaveid = it["id"].asText() // TODO: NPE-hantering?
                            logger.info("Hentet existerande oppgave $oppgaveid")
                            val journalpostId = it["journalpostId"].asText()
                            journalpostId to oppgaveid
                        }.toMap()
                    }
                    HttpStatusCode.Created -> { // Håndter OpprettOppgave
                        val response = objectMapper.readValue<OppgaveRespons>(response.content.toByteArray())

                        if (response.id.isNullOrEmpty()) {
                            throw RuntimeException("Uventet feil vid parsing av svar fra oppgave api, id er null")
                        }
                        logger.info("Opprettet oppgave ${response.id}")
                        return mapOf(response.journalpostId to response.id)
                    }
                    else -> {
                        response.logError()
                        throw RuntimeException("Uventet response code (${response.status}) fra oppgave-api")
                    }
                }
            },
            onFailure = { cause ->
                when (cause is ResponseException) {
                    true -> {
                        cause.response.logError()
                        throw RuntimeException("Uventet feil ved kall till oppgave-api")
                    }
                    else -> throw cause
                }
            }
    )

    private suspend fun HttpResponse.logError() =
            logger.error("HTTP ${status.value} fra oppgave-api, response: ${String(content.toByteArray())}")

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
        private val logger = LoggerFactory.getLogger(Oppgave::class.java)

        val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
    }
}

typealias OppgaveLøsning = Map<String, String>