package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language

private const val basePath = "/journalpostapi-mock"
private const val journalpostPath = "$basePath/rest/journalpostapi/v1/journalpost"

private fun oppdaterJournalpostMapping(
    callIdPattern: StringValuePattern = AnythingPattern()
) = WireMock.put(WireMock
    .urlMatching(".*$journalpostPath.*"))
    .withHeader("Authorization", RegexPattern("^Bearer .+$"))
    .withHeader("Content-Type", equalTo("application/json"))
    .withHeader("Nav-Consumer-Id", equalTo("omsorgspenger-journalforing"))
    .withHeader("Nav-Callid", callIdPattern)
    .withRequestBody(matchingJsonPath("$.sak.sakstype", equalTo("FAGSAK")))
    .withRequestBody(matchingJsonPath("$.sak.fagsaksystem", WireMock.matching("K9|OMSORGSPENGER")))
    .withRequestBody(matchingJsonPath("$.tema", equalTo("OMS")))
    .withRequestBody(matchingJsonPath("$.bruker.idType", equalTo("FNR")))
    .withRequestBody(matchingJsonPath("$.bruker.id"))

private fun ferdigstillJournalpostMapping(
    callIdPattern: StringValuePattern = AnythingPattern()
) = WireMock.patch(WireMock
    .urlPathMatching(".*$journalpostPath.*"))
    .withHeader("Authorization", RegexPattern("^Bearer .+$"))
    .withHeader("Content-Type", equalTo("application/json"))
    .withHeader("Nav-Consumer-Id", equalTo("omsorgspenger-journalforing"))
    .withHeader("Nav-Callid", callIdPattern)
    .withRequestBody(matchingJsonPath("$.journalfoerendeEnhet", containing("9999")))

private fun WireMockServer.stubOppdaterJournalpostOk() = also {
    stubFor(oppdaterJournalpostMapping()
        .willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
}

private fun WireMockServer.stubOppdaterJournalpostUventetFeil() = also {
    stubFor(oppdaterJournalpostMapping(callIdPattern = equalTo("400"))
        .willReturn(
            WireMock.aResponse()
                .withStatus(400)
                .withBody("Noe gikk helt galt")
        )
    )
}

private fun WireMockServer.stubOppdaterJournalpostAlleredeFerdigstilt() = also {
    @Language("JSON")
    val json = """
        {
        	"timestamp": "2020-10-30T12:30:06.929+00:00",
        	"status": 400,
        	"error": "Bad Request",
        	"message": "Bruker kan ikke oppdateres for journalpost med journalpostStatus=J og journalpostType=I.",
        	"path": "/rest/journalpostapi/v1/journalpost/111111111"
        }
    """.trimIndent()
    stubFor(oppdaterJournalpostMapping(callIdPattern = equalTo("allerede-ferdigstilt"))
        .willReturn(
            WireMock.aResponse()
                .withStatus(400)
                .withBody(json)
        )
    )
}

private fun WireMockServer.stubFerdigstillJournalpostOk() = also {
    stubFor(ferdigstillJournalpostMapping()
        .willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
}

private fun WireMockServer.stubFerdigstillJournalpostUventetFeil() = also {
    stubFor(ferdigstillJournalpostMapping(callIdPattern = equalTo("feil-ved-ferdigstilling"))
        .willReturn(
            WireMock.aResponse()
                .withStatus(400)
        )
    )
}


private fun WireMockServer.stubIsReady() = also {
    stubFor(WireMock.get("$basePath/isReady")
        .willReturn(WireMock.aResponse()
            .withStatus(200)
        )
    )
}

internal fun WireMockServer.stubJournalpostApi() =
    stubOppdaterJournalpostOk()
    .stubOppdaterJournalpostUventetFeil()
    .stubOppdaterJournalpostAlleredeFerdigstilt()
    .stubFerdigstillJournalpostOk()
    .stubFerdigstillJournalpostUventetFeil()
    .stubIsReady()

internal fun WireMockServer.journalpostApiBaseUrl() = baseUrl() + basePath