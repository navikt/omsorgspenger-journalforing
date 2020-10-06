package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.ServiceUser
import no.nav.omsorgspenger.StsRestClient
import no.nav.omsorgspenger.testutils.TestApplicationEngineExtension
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationEngineExtension::class)
internal class JoarkClientTest(
        private val wireMockServer: WireMockServer) {

    internal val objectMapper: ObjectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

    val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }

    private val client = JoarkClient(
        baseUrl = wireMockServer.journalpostApiBaseUrl(),
        httpClient = httpClient,
        stsRestClient = StsRestClient(
            baseUrl = wireMockServer.getNaisStsTokenUrl(),
            serviceUser = ServiceUser("foo", "bar"),
            httpClient = httpClient
        )
    )

    @Test
    fun `ferdigstill journalpost test` () {

        val hendelseId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.ferdigstillJournalpost(
                hendelseId = hendelseId,
                journalpostPayload = JournalpostPayload(
                    journalpostId = "123",
                    bruker = JournalpostPayload.Bruker(id = "12312312311"),
                    sak = JournalpostPayload.Sak(fagsakId = "123")
                )
            )
        }

        assertTrue(result)
    }

    @Test
    fun `oppdater journalpost test` () {

        val hendelseId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.oppdaterJournalpost(
                    hendelseId = hendelseId,
                    journalpostPayload = JournalpostPayload(
                            journalpostId = "123",
                            bruker = JournalpostPayload.Bruker(id = "12312312311"),
                            sak = JournalpostPayload.Sak(fagsakId = "123")
                    )
            )
        }

        assertTrue(result)
    }
}