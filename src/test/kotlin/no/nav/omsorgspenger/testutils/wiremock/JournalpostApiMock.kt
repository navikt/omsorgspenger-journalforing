package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val journalpostApiMockPath = "/rest/journalpostapi/v1/"

private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    WireMock.stubFor(
            //WireMock.any(WireMock.urlPathMatching(".*$journalpostApiMockPath.*"))
            WireMock.any(WireMock.anyUrl())
                    //.withHeader("x-nav-apiKey", AnythingPattern())
                    .willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
            )
    )
    return this
}

internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost()