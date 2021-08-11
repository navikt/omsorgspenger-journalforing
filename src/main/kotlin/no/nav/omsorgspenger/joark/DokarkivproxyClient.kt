package no.nav.omsorgspenger.joark

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPut
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.omsorgspenger.AzureAwareClient
import no.nav.omsorgspenger.CorrelationId
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import java.net.URI

internal class DokarkivproxyClient(
    accessTokenClient: AccessTokenClient,
    private val baseUrl: URI,
    scopes: Set<String>) : AzureAwareClient(
        navn = "DokarkivproxyClient",
        accessTokenClient = accessTokenClient,
        scopes = scopes,
        pingUrl = URI("$baseUrl/isReady")) {

    private fun knyttTilAnnenSakUrl(journalpostId: String) =
        "$baseUrl/rest/journalpostapi/v1/journalpost/$journalpostId/knyttTilAnnenSak"

    internal suspend fun knyttTilAnnenSak(
        correlationId: CorrelationId,
        journalpost: Journalpost
    ) : JournalpostId {

        @Language("JSON")
        val dto = """
        {
            "sakstype": "FAGSAK",
            "fagsaksystem": "${journalpost.fagsaksystem.name}",
            "fagsakId": "${journalpost.saksnummer}",
            "journalfoerendeEnhet": "9999",
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "${journalpost.identitetsnummer}"
            }
        }
        """.trimIndent()

        val url = knyttTilAnnenSakUrl(journalpostId = journalpost.journalpostId)
        val (httpStatusCode, response) = url.httpPut { builder->
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
}