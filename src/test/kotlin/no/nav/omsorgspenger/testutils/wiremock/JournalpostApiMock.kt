package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.intellij.lang.annotations.Language

private const val journalpostApiMockPath = "/rest/journalpostapi/v1-mock"

private fun WireMockServer.stubOppdaterJournalpost(): WireMockServer {
    @Language("JSON")
    val response = """
    {}
        """.trimIndent()
    WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(".*$journalpostApiMockPath.*")).willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
                            .withBody(response)
            )
    )
    return this
}

internal fun WireMockServer.stubJournalpostApi() = stubOppdaterJournalpost()
internal fun WireMockServer.journalpostApiBaseUrl() = baseUrl() + journalpostApiMockPath