package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern

private const val journalpostApiBasePath = "/journalpostapi-mock"
private const val journalpostApiMockPath = "/rest/journalpostapi/v1/journalpost"
private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    WireMock.stubFor(
            WireMock.put(WireMock
                    .urlPathMatching(".*$journalpostApiMockPath.*"))
                    .withHeader("Authorization", RegexPattern("^Bearer .+$"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Id", equalTo("omsorgspenger-journalforing"))
                    .withHeader("Nav-Callid", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.journalpostId"))
                    .withRequestBody(matchingJsonPath("$.sak.sakstype", equalTo("FAGSAK")))
                    .withRequestBody(matchingJsonPath("$.sak.fagsaksystem", equalTo("OMSORGSPENGER")))
                    .withRequestBody(matchingJsonPath("$.tema", equalTo("OMS")))
                    .withRequestBody(matchingJsonPath("$.bruker.idType", equalTo("FNR")))
                    .withRequestBody(matchingJsonPath("$.bruker.id"))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                    )
    )

    return this
}

private fun WireMockServer.stubFerdigstillJournalpost(): WireMockServer {
    WireMock.stubFor(
            WireMock.patch(WireMock
                    .urlPathMatching(".*$journalpostApiMockPath.*"))
                    .withHeader("Authorization", RegexPattern("^Bearer .+$"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Id", equalTo("omsorgspenger-journalforing"))
                    .withHeader("Nav-Callid", AnythingPattern())
                    .withRequestBody(matchingJsonPath("$.journalfoerendeEnhet", containing("9999")))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                    )
    )
    return this
}

private fun WireMockServer.stubKaste400(): WireMockServer {
    WireMock.stubFor(
            WireMock.put(WireMock
                    .urlPathMatching(".*$journalpostApiMockPath.*"))
                    .withHeader("Authorization", RegexPattern("^Bearer .+$"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("Nav-Consumer-Id", equalTo("omsorgspenger-journalforing"))
                    .withHeader("Nav-Callid", equalTo("400"))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(400)
                                    .withBody("Fick 400 feil fra dokarkiv!")
                    )
    )
    return this
}

internal fun WireMockServer.stubIsReady(): WireMockServer {
    stubFor(WireMock.get("$journalpostApiBasePath/isReady")
            .willReturn(WireMock.aResponse()
                    .withStatus(200)
            ))
    return this
}


internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost().stubFerdigstillJournalpost().stubIsReady().stubKaste400()
internal fun WireMockServer.journalpostApiBaseUrl() = baseUrl() + journalpostApiBasePath