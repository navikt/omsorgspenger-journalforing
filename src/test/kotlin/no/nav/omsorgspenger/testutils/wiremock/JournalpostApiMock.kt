package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val journalpostApiMockPath = "/rest/journalpostapi/v1/"

private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    WireMock.stubFor(
            WireMock.put(WireMock.urlPathMatching(".*$journalpostApiMockPath.*"))
                    .willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
            )
    )
    return this
}

internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost()