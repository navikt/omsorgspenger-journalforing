package no.nav.omsorgspenger.journalforing

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.*
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.ServiceUser
import no.nav.omsorgspenger.StsRestClient
import no.nav.omsorgspenger.testutils.TestApplicationEngineExtension
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(TestApplicationEngineExtension::class)
internal class FerdigstillJournalforingTest(
        private val wireMockServer: WireMockServer) {

    private val client = JoarkClient(
        baseUrl = wireMockServer.journalpostApiBaseUrl(),
        httpClient = HttpClient(),
        stsRestClient = StsRestClient(
            baseUrl = wireMockServer.getNaisStsTokenUrl(),
            serviceUser = ServiceUser("foo", "bar"),
            httpClient = HttpClient()
        )
    )

    @Test
    fun `Oppdater metadata på journalpost med gyldig data` () {

        val hendelseId = UUID.randomUUID()

        val result = runBlocking {
            client.ferdigstillJournalpost(
                hendelseId = hendelseId,
                behovPayload = BehovPayload(
                    hendelseId = hendelseId,
                    journalpostId = "123",
                    tema = "OMS",
                    journalfoerendeEnhet = "9999",
                    bruker = BehovPayload.Bruker(id = "12312312311"),
                    sak = BehovPayload.Sak(fagsakId = "123")
                )
            )
        }

        assertTrue(result)




        /*
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Put, "/rest/journalpostapi/v1/123456789") {
                addHeader("Content-Type", "application/json")
                addHeader("Nav-Consumer-Token", "consumer")
                addHeader("X-Correlation-Id", UUID.randomUUID().toString())
                addHeader("nav-x-apiKey", "thisisatest")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                setBody("""
                {
                    "bruker": {
                        "id": "11111111111"
                    }
                    "sak": {
                        "fagsakId": "a1b2c3"
                    }
                }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }

         */
    }
}

internal fun gyldigToken() = Azure.V2_0.generateJwt(
        clientId = "any",
        audience = "omsorgspenger-journalforing",
        clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET
)