package no.nav.omsorgspenger.journalforing

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.*
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.ZonedDateTime
import java.util.*

internal class JournalforingFeilhandteringTest {

    private val mockJoarkClient = mockk<JoarkClient>()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(ApplicationContext.Builder(
            env = mapOf(
                "JOARK_BASE_URL" to "test",
                "DOKARKIV_SCOPES" to "testScope/.default",
                "OPPGAVE_BASE_URL" to "test",
                "OPPGAVE_SCOPES" to "test/.default"
            ),
            accessTokenClient =  ClientSecretAccessTokenClient(
                clientId = "omsorgspenger-journalforing",
                clientSecret = "azureSecret",
                tokenEndpoint = URI("test")
            ),
            joarkClient = mockJoarkClient,
            dokarkivproxyClient = mockk(),
            safGateway = mockk()
        ).build())
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Håndterer feil i ferdigstilling och går igenom etter nytt försök`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                journalpostIder = setOf("11111", "22222")
        )

        val identitetsnummer = "11111111111"
        val saksnummer = "a1b2c3"

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), any()) }
            .returns(JournalpostStatus.Oppdatert)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), journalpostId = "11111") }
            .returns(JournalpostStatus.Ferdigstilt)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), journalpostId = "22222") }
            .returns(JournalpostStatus.Feilet)

        rapid.sendTestMessage(behovssekvens)

        assert(rapid.inspektør.size == 1)
        assertFalse(rapid.inspektør.message(0).get("@behov").get("HentPersonopplysninger@journalføring").isMissingOrNull())
        assertNull(rapid.inspektør.message(0).get("@løsninger"))

        clearMocks(mockJoarkClient)
        val navn = "LITEN MASKIN"

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), Journalpost(
            journalpostId = "11111",
            identitetsnummer = identitetsnummer,
            saksnummer = saksnummer,
            fagsaksystem = Fagsystem.OMSORGSPENGER,
            navn = navn
        ))}.returns(JournalpostStatus.Ferdigstilt)

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), Journalpost(
            journalpostId = "22222",
            identitetsnummer = identitetsnummer,
            saksnummer = saksnummer,
            fagsaksystem = Fagsystem.OMSORGSPENGER,
            navn = navn
        ))}.returns(JournalpostStatus.Oppdatert)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), any()) }.returns(JournalpostStatus.Ferdigstilt)

        val behovssekvensMedNavn = JSONObject(rapid.inspektør.message(0).toString()).also { b ->
            val løsninger = """
            {
                "HentPersonopplysninger@journalføring": {
                    "personopplysninger": {
                        "$identitetsnummer": {
                            "navn": {
                                "sammensatt": "$navn"
                            }
                        }
                    }
                }
            }
            """.trimIndent().let { JSONObject(it) }
            b.put("@løsninger", løsninger)
        }.toString()

        rapid.sendTestMessage(behovssekvensMedNavn)

        assertNotNull(rapid.løst())
    }

    private fun TestRapid.løst() = inspektør.message(1)
        .get("@løsninger")
        .get(BEHOV)
        .get("løst")
        .asText()
        .let { ZonedDateTime.parse(it) }


    internal companion object {
        const val BEHOV = "FerdigstillJournalføringForOmsorgspenger"

        private fun nyBehovsSekvens(
                id: String,
                journalpostIder: Set<String>,
                correlationId: String = UUID.randomUUID().toString()
        ) = Behovssekvens(
            id = id,
            correlationId = correlationId,
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