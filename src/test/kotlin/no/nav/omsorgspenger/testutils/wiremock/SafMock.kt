package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import org.intellij.lang.annotations.Language

private const val path = "/saf-mock"
@Language("JSON")
private val IngenJournalposterResponse = """
    {
      "data": {
        "dokumentoversiktFagsak": {
          "journalposter": []
        }
      }
    }
""".trimIndent()

private fun WireMockServer.mockHentOriginaleJournalpostIder(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$path/graphql"))
            .withHeader("Authorization", WireMock.containing("Bearer e"))
            .withHeader("Accept", WireMock.equalTo("application/json"))
            .withHeader("Nav-Consumer-Id", WireMock.equalTo("omsorgspenger-journalforing"))
            .withHeader("Nav-Callid", AnythingPattern())
            .withHeader("Content-Type", WireMock.equalTo("application/json"))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(IngenJournalposterResponse)
            )
    )
    return this
}

internal fun WireMockServer.stubSaf() = mockHentOriginaleJournalpostIder()
internal fun WireMockServer.safBaseUrl() = baseUrl() + path
