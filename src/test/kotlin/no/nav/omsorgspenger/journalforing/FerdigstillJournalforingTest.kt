package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.ServiceUser
import no.nav.omsorgspenger.StsRestClient
import no.nav.omsorgspenger.testutils.TestApplicationEngineExtension
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationEngineExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FerdigstillJournalforingTest(
        private val wireMockServer: WireMockServer) {

    internal val objectMapper: ObjectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

    val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }

    private val joarkClient = JoarkClient(
            baseUrl = wireMockServer.journalpostApiBaseUrl(),
            httpClient = httpClient,
            stsRestClient = StsRestClient(
                    baseUrl = wireMockServer.getNaisStsTokenUrl(),
                    serviceUser = ServiceUser("foo", "bar"),
                    httpClient = httpClient
            )
    )
    private var rapid = TestRapid()
    private val journalforingMediator = JournalforingMediator(joarkClient)

    @BeforeAll
    internal fun setup() {
        rapid.apply {
            FerdigstillJournalforing(this, journalforingMediator)
        }
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Godtar requests med flera journalpostIder`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                journalpostIder = setOf("123abc", "345def")
        )
        rapid.sendTestMessage(behovssekvens)

        assertEquals(1, rapid.inspektør.size)
    }

    internal companion object {
        const val BEHOV = "FerdigstillJournalføringForOmsorgspenger"

        private fun nyBehovsSekvens(
                id: String,
                journalpostIder: Set<String>
        ) = Behovssekvens(
                id = id,
                correlationId = UUID.randomUUID().toString(),
                behov = arrayOf(
                        Behov(
                                navn = BEHOV,
                                input = mapOf(
                                        "identitetsnummer" to "11111111111",
                                        "journalpostIder" to journalpostIder,
                                        "saksnummer" to "a1b2c3"
                                )
                        )
                )
        ).keyValue

    }

}