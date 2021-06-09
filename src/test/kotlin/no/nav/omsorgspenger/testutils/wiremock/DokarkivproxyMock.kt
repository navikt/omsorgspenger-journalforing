package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern

private const val basePath = "/dokarkivproxy-mock"
private const val journalpostPath = "$basePath/rest/journalpostapi/v1/journalpost/.*"

private fun WireMockServer.stubKnyttTilAnnenSakK9() = also { wireMockServer ->
    wireMockServer.stubFor(WireMock.put(WireMock.urlMatching(".*$journalpostPath.*"))
        .withHeader("Authorization", RegexPattern("^Bearer .+$"))
        .withHeader("Content-Type", WireMock.equalTo("application/json"))
        .withHeader("Accept", WireMock.equalTo("application/json"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("omsorgspenger-journalforing"))
        .withHeader("Nav-Callid", AnythingPattern())
        .withRequestBody(WireMock.matchingJsonPath("$.sakstype", WireMock.equalTo("FAGSAK")))
        .withRequestBody(WireMock.matchingJsonPath("$.fagsaksystem", WireMock.equalTo("K9")))
        .withRequestBody(WireMock.matchingJsonPath("$.tema", WireMock.equalTo("OMS")))
        .withRequestBody(WireMock.matchingJsonPath("$.journalfoerendeEnhet", WireMock.equalTo("9999")))
        .withRequestBody(WireMock.matchingJsonPath("$.bruker.idType", WireMock.equalTo("FNR")))
        .withRequestBody(WireMock.matchingJsonPath("$.bruker.id"))
        .willReturn(WireMock.aResponse().withStatus(200).withBody("""{"nyJournalpostId":"123412341234"}""")))
}

private fun WireMockServer.stubIsReady() = also { wireMockServer ->
    wireMockServer.stubFor(
        WireMock.get("$basePath/isReady")
        .willReturn(
            WireMock.aResponse()
            .withStatus(200)
        )
    )
}

internal fun WireMockServer.stubDokarkivproxy() = stubIsReady().stubKnyttTilAnnenSakK9()
internal fun WireMockServer.dokarkivproxyBaseUrl() = baseUrl() + basePath