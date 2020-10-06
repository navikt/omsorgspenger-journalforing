package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

private const val journalpostApiBasePath = "/journalpostapi-mock"
private const val journalpostApiMockPath = "/rest/journalpostapi/v1/"

private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    WireMock.stubFor(
            WireMock.any(WireMock.urlPathMatching(".*$journalpostApiMockPath.*"))
                    .willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
            )
    )
    return this
}

internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost()
internal fun WireMockServer.journalpostApiBaseUrl() = baseUrl() + journalpostApiBasePath