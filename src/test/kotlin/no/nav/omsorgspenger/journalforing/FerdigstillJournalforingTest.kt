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
import java.util.UUID

@ExtendWith(TestApplicationEngineExtension::class)
internal class FerdigstillJournalforingTest(
        private val testApplicationEngine: TestApplicationEngine) {

    @Test
    fun `Oppdater metadata på journalpost med gyldig data` () {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/rammevedtak") {
                addHeader("Content-Type", "application/json")
                addHeader("X-Correlation-Id", UUID.randomUUID().toString())
                addHeader("nav-x-apiKey", "thisisatest")
                addHeader("Authorization", "Bearer ${gyldigToken()}")
                setBody("""
                {
                    "personIdent": "29099011111",
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