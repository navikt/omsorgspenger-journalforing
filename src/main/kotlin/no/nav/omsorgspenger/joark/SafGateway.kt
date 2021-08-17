package no.nav.omsorgspenger.joark

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgspenger.AzureAwareClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostType.Companion.somJournalpostType
import org.json.JSONObject
import java.net.URI
import java.time.LocalDate

internal class SafGateway(
    accessTokenClient: AccessTokenClient,
    baseUrl: URI,
    scopes: Set<String>) : AzureAwareClient(
        navn = "SafGateway",
        accessTokenClient = accessTokenClient,
        scopes = scopes,
        pingUrl = URI("$baseUrl/isReady")) {

    private val GraphQlUrl = URI("$baseUrl/graphql")

    internal suspend fun hentOriginaleJournalpostIder(
        fagsystem: Fagsystem,
        saksnummer: Saksnummer,
        fraOgMed: LocalDate,
        correlationId: CorrelationId
    ) : Map<JournalpostId, Set<JournalpostId>> {

        val (httpStatusCode, response) = GraphQlUrl.httpPost { builder->
            builder.header("Nav-CallId", "$correlationId")
            builder.header("Nav-Consumer-Id", "omsorgspenger-journalforing")
            builder.accept(ContentType.Application.Json)
            builder.header(HttpHeaders.Authorization, authorizationHeader())
            builder.jsonBody(
                hentOriginalJournalpostIderQuery(
                    fagsystem = fagsystem,
                    saksnummer = saksnummer,
                    fraOgMed = fraOgMed
                )
            )
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "Feil fra SAF. URL=[$GraphQlUrl], HttpStatusCode=[${httpStatusCode.value}], Response=[$response]"
        }

        return response.mapOriginaleJournalpostIderResponse()
    }

    internal suspend fun hentTypeOgStatus(
        journalpostId: JournalpostId,
        correlationId: CorrelationId) : Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus> {
        return "I".somJournalpostType() to "JOURNALFOERT".somJournalpostStatus()
    }

    internal companion object {
        private const val MaksAntallJournalposter = 50

        internal fun hentOriginalJournalpostIderQuery(fagsystem: Fagsystem, saksnummer: Saksnummer, fraOgMed: LocalDate) = """
            {"query":"query {dokumentoversiktFagsak(tema:OMS,fagsak:{fagsaksystem:\"${fagsystem.name}\",fagsakId:\"${saksnummer}\"},foerste:$MaksAntallJournalposter,fraDato:\"$fraOgMed\"){journalposter{journalpostId,dokumenter{originalJournalpostId}}}}"}
        """.trimIndent()

        private fun JSONObject.notNullNotBlankString(key: String)
            = has(key) && get(key) is String && getString(key).isNotBlank()

        private fun JSONObject.originaleJournalpostIder() = getJSONArray("dokumenter")
            .map { it as JSONObject }.mapNotNull {
                when (it.notNullNotBlankString("originalJournalpostId")) {
                    true -> it.getString("originalJournalpostId").somJournalpostId()
                    false -> null
                }
            }

        internal fun Map<JournalpostId, Set<JournalpostId>>.førsteJournalpostIdSomHarOriginalJournalpostId(originalJournalpostId: JournalpostId) =
            filterValues { it.contains(originalJournalpostId) }.keys.firstOrNull()

        internal fun String.mapOriginaleJournalpostIderResponse() = JSONObject(this)
            .getJSONObject("data")
            .getJSONObject("dokumentoversiktFagsak")
            .getJSONArray("journalposter")
            .asSequence()
            .map { it as JSONObject }
            .map { it.getString("journalpostId").somJournalpostId() to it.originaleJournalpostIder() }
            .toMap()
            .mapValues { it.value.toSet() }
            .also { require(it.size < MaksAntallJournalposter) {
                "Fant ${it.size} journalposter, støtter maks $MaksAntallJournalposter"
            }}
    }
}