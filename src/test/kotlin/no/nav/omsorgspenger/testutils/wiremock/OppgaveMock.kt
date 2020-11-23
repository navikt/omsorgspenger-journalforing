package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language

private const val basePath = "/oppgave-mock"
private const val oppgaveApiPath = "$basePath/api/v1/oppgaver"

private fun opprettOppgaveMapping(
        callIdPattern: StringValuePattern = AnythingPattern()
) = post(WireMock
        .urlPathMatching(".*$oppgaveApiPath.*"))
        .withHeader("Authorization", RegexPattern("^Bearer .+$"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("X-Correlation-ID", callIdPattern)

private fun hentOppgaveMapping(
        callIdPattern: StringValuePattern = AnythingPattern()
) = get(WireMock
        .urlPathMatching(".*$oppgaveApiPath.*"))
        .withHeader("Authorization", RegexPattern("^Bearer .+$"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("X-Correlation-ID", callIdPattern)

private fun WireMockServer.stubHentOppgaveOK() = also {
    @Language("JSON")
    val json = """
    {
      "antallTreffTotalt": "1",
      "oppgaver": [
        {
            "id": "5436732",
            "tildeltEnhetsnr": "1234",
            "behandlingsTema": "abc123"
        }
      ]
    }
    """.trimIndent()
    stubFor(hentOppgaveMapping()
            .willReturn(
                    WireMock.aResponse()
                            .withStatus(200)
                            .withBody(json)
            )
    )
}

private fun WireMockServer.stubOpprettOppgaveCreated() = also {
    @Language("JSON")
    val json = """
        	{
        	  "id": "1",
        	  "aktoerId": "11111111111",
        	  "journalpostId": "1234567",
        	  "tema": "OMS",
        	  "prioritet": "NORM",
        	  "aktivDato": "2000-01-01"
        	}
    """.trimIndent()
    stubFor(opprettOppgaveMapping()
            .willReturn(
                    WireMock.aResponse()
                            .withStatus(201)
                            .withBody(json)
            )
    )
}

internal fun WireMockServer.stubOppgaveMock() =
        stubOpprettOppgaveCreated()
        .stubHentOppgaveOK()

internal fun WireMockServer.oppgaveApiBaseUrl() = baseUrl() + basePath