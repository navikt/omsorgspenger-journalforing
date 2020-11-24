package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
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
      "antallTreffTotalt": "2",
      "oppgaver": [
        {
            "id": "HentOppgaveId1",
            "journalpostId": "HentJournalpostId1",
            "behandlingsTema": "test"
        },
        {
            "id": "HentOppgaveId2",
            "journalpostId": "HentJournalpostId2",
            "behandlingsTema": "test2"
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
        	  "id": "OpprettOppgaveId1",
        	  "aktoerId": "11111111111",
        	  "journalpostId": "OpprettJournalpostId1",
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

private fun WireMockServer.stubIsReady() = also {
    stubFor(WireMock.get("$basePath/isReady")
            .willReturn(WireMock.aResponse()
                    .withStatus(200)
            )
    )
}

internal fun WireMockServer.stubOppgaveMock() =
        stubOpprettOppgaveCreated().stubHentOppgaveOK().stubIsReady()

internal fun WireMockServer.oppgaveApiBaseUrl() = baseUrl() + basePath