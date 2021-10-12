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

    private suspend fun String.hentDataFraSaf(correlationId: CorrelationId) : JSONObject {
        val (httpStatusCode, response) = GraphQlUrl.httpPost { builder->
            builder.header("Nav-Callid", "$correlationId")
            builder.header("Nav-Consumer-Id", "omsorgspenger-journalforing")
            builder.header(HttpHeaders.Authorization, authorizationHeader())
            builder.accept(ContentType.Application.Json)
            builder.jsonBody(this)
        }.readTextOrThrow()

        check(httpStatusCode.isSuccess()) {
            "Feil fra SAF. Url=[$GraphQlUrl], HttpStatus=[${httpStatusCode.value}], Request=[$this], Response=[$response]"
        }

        return response.safData()
    }

    internal suspend fun hentOriginaleJournalpostIder(
        fagsystem: Fagsystem,
        saksnummer: Saksnummer,
        fraOgMed: LocalDate,
        correlationId: CorrelationId
    ) : Map<JournalpostId, Set<JournalpostId>> {

        val request = hentOriginalJournalpostIderQuery(
            fagsystem = fagsystem,
            saksnummer = saksnummer,
            fraOgMed = fraOgMed
        )

        return request.hentDataFraSaf(correlationId).mapOriginaleJournalpostIderResponse()
    }

    internal suspend fun hentTypeOgStatus(
        journalpostId: JournalpostId,
        correlationId: CorrelationId) : Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus> {

        val request = hentTypeOgStatusQuery(
            journalpostId = journalpostId
        )

        return request.hentDataFraSaf(correlationId).mapTypeOgStatus()
    }

    internal suspend fun hentFerdigstillJournalpost(
        correlationId: CorrelationId,
        journalpostId: JournalpostId
    ) : FerdigstillJournalpost {

        val request = hentFerdigstillJournalpostQuery(
            journalpostId = journalpostId
        )

        return request.hentDataFraSaf(correlationId).mapFerdigstillJournalpost(journalpostId)
    }

    internal companion object {
        private const val MaksAntallJournalposter = 50

        internal fun hentOriginalJournalpostIderQuery(fagsystem: Fagsystem, saksnummer: Saksnummer, fraOgMed: LocalDate) = """
            {"query":"query {dokumentoversiktFagsak(tema:OMS,fagsak:{fagsaksystem:\"${fagsystem.name}\",fagsakId:\"${saksnummer}\"},foerste:$MaksAntallJournalposter,fraDato:\"$fraOgMed\"){journalposter{journalpostId,dokumenter{originalJournalpostId}}}}"}
        """.trimIndent()

        internal fun hentTypeOgStatusQuery(journalpostId: JournalpostId) = """
            {"query":"query {journalpost(journalpostId:\"${journalpostId}\"){journalposttype,journalstatus}}"}
        """.trimIndent()

        internal fun hentFerdigstillJournalpostQuery(journalpostId: JournalpostId) = """
            {"query":"query {journalpost(journalpostId:\"${journalpostId}\"){journalstatus,journalposttype,tittel,avsenderMottaker{navn},dokumenter{dokumentInfoId,tittel}}}"}
        """.trimIndent()

        private fun JSONObject.notNullNotBlankString(key: String)
            = has(key) && get(key) is String && getString(key).isNotBlank()

        private fun JSONObject.stringOrNull(key: String) = when (notNullNotBlankString(key)) {
            true -> getString(key)
            false -> null
        }

        private fun JSONObject.originaleJournalpostIder() =
            getJSONArray("dokumenter")
            .map { it as JSONObject }
            .mapNotNull { it.stringOrNull("originalJournalpostId")?.somJournalpostId() }

        internal fun Map<JournalpostId, Set<JournalpostId>>.førsteJournalpostIdSomHarOriginalJournalpostId(originalJournalpostId: JournalpostId) =
            filterKeys { it != originalJournalpostId }.filterValues { it.contains(originalJournalpostId) }.keys.firstOrNull()

        internal fun String.safData() = JSONObject(this).getJSONObject("data")

        internal fun JSONObject.mapOriginaleJournalpostIderResponse() =
            getJSONObject("dokumentoversiktFagsak")
            .getJSONArray("journalposter")
            .asSequence()
            .map { it as JSONObject }
            .map { it.getString("journalpostId").somJournalpostId() to it.originaleJournalpostIder() }
            .toMap()
            .mapValues { it.value.toSet() }
            .also { require(it.size < MaksAntallJournalposter) {
                "Fant ${it.size} journalposter, støtter maks $MaksAntallJournalposter"
            }}

        internal fun JSONObject.mapTypeOgStatus() =
            getJSONObject("journalpost")
            .let { journalpost ->
                journalpost.getString("journalposttype").somJournalpostType() to journalpost.getString("journalstatus").somJournalpostStatus()
            }

        internal fun JSONObject.mapFerdigstillJournalpost(journalpostId: JournalpostId) =
            getJSONObject("journalpost")
                .let { journalpost -> FerdigstillJournalpost(
                    journalpostId = journalpostId,
                    avsendernavn = journalpost.getJSONObject("avsenderMottaker").stringOrNull("navn"),
                    status = journalpost.getString("journalstatus").somJournalpostStatus(),
                    type = journalpost.getString("journalposttype").somJournalpostType(),
                    tittel = journalpost.stringOrNull("tittel"),
                    dokumenter = journalpost.getJSONArray("dokumenter").map { it as JSONObject }.map { FerdigstillJournalpost.Dokument(
                        dokumentId = it.getString("dokumentInfoId"),
                        tittel = it.stringOrNull("tittel")
                    )}.toSet()
                )}
    }
}