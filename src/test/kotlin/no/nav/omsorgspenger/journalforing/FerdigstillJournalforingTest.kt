package no.nav.omsorgspenger.journalforing

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgspenger.testutils.TestApplicationEngineExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestApplicationEngineExtension::class)
internal class HenteRammevedtakTest(
        private val testApplicationEngine: TestApplicationEngine) {

    @Test
    fun `Oppdater metadata på journalpost med gyldig data` () {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Put, "/rest/journalpostapi/v1/journalpost/12345123451234") {
                addHeader("Content-Type", "application/json")
                addHeader("X-Correlation-Id", UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                setBody("""
                    {
                        "journalpostId": "12345678""
                        "tema": "OMS",
                        "behandlingstema": "ab0061",
                        "bruker": {
                            "id": "01019911111",
                            "idType": "FNR"
                        },
                        "sak": {
                            "fagsakId": "k5c81d",
                            "fagsaksystem": "K9",
                            "sakstype": "FAGSAK"
                        }
                    }
                """.trimIndent())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}

internal fun gyldigToken() = Azure.V2_0.generateJwt(
        clientId = "any",
        audience = "omsorgspenger-journalforing",
        clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET
)