package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val journalpostApiBasePath = "/journalpostapi-mock"
private const val journalpostApiMockPath = "/rest/journalpostapi/v1/journalpost"

private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    WireMock.stubFor(
            WireMock.put(WireMock
                    .urlPathMatching(".*$journalpostApiMockPath.*"))
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
            )
    )

    return this
}

private fun WireMockServer.stubFerdigstillJournalpost(): WireMockServer {
    val jsonBody = """
                {"journalfoerendeEnhet":"9999"}
            """.trimIndent()

    WireMock.stubFor(
            WireMock.patch(WireMock
                    .urlPathMatching(".*$journalpostApiMockPath.*"))
                    .withHeader("x-nav-apiKey", AnythingPattern())
                    .withRequestBody(matchingJsonPath(jsonBody))
                    .willReturn(
                            WireMock.aResponse()
                                    .withStatus(200)
                    )
    )
    return this
}


internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost().stubFerdigstillJournalpost()
internal fun WireMockServer.journalpostApiBaseUrl() = baseUrl() + journalpostApiBasePath