package no.nav.omsorgspenger.testutils.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.intellij.lang.annotations.Language

private const val basePath = "/oppgave-mock"
private const val oppgaveApiPath = "$basePath/api/v1/oppgaver"

private fun oppdaterJournalpostMapping(
        callIdPattern: StringValuePattern = AnythingPattern()
) = post(WireMock
        .urlPathMatching(".*$oppgaveApiPath.*"))
        .withHeader("Authorization", RegexPattern("^Bearer .+$"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withHeader("X-Correlation-ID", callIdPattern)

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
    stubFor(oppdaterJournalpostMapping(callIdPattern = equalTo("hantererbehov"))
            .willReturn(
                    WireMock.aResponse()
                            .withStatus(201)
                            .withBody(json)
            )
    )
}

internal fun WireMockServer.stubOppgaveMock() =
        stubOpprettOppgaveCreated()

internal fun WireMockServer.oppgaveApiBaseUrl() = baseUrl() + basePath



/*


 */