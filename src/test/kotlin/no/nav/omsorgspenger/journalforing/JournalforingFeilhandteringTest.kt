package no.nav.omsorgspenger.journalforing

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.JournalpostStatus
import no.nav.omsorgspenger.registerApplicationContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertNotNull

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
                joarkClient = mockJoarkClient
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
                journalpostIder = setOf("1111", "2222")
        )

        val identitetsnummer = "11111111111"
        val saksnummer = "a1b2c3"

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), any()) }
            .returns(JournalpostStatus.Oppdatert)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), journalpostId = "1111") }
            .returns(JournalpostStatus.Ferdigstilt)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), journalpostId = "2222") }
            .returns(JournalpostStatus.Feilet)

        rapid.sendTestMessage(behovssekvens)

        assert(rapid.inspektør.size == 0)

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), Journalpost(
            journalpostId = "1111",
            identitetsnummer = identitetsnummer,
            saksnummer = saksnummer
        ))
        }.returns(JournalpostStatus.Ferdigstilt)

        coEvery { mockJoarkClient.oppdaterJournalpost(any(), Journalpost(
            journalpostId = "2222",
            identitetsnummer = identitetsnummer,
            saksnummer = saksnummer
        ))
        }.returns(JournalpostStatus.Oppdatert)

        coEvery { mockJoarkClient.ferdigstillJournalpost(any(), any()) }
            .returns(JournalpostStatus.Ferdigstilt)

        rapid.sendTestMessage(behovssekvens)

        assertNotNull(rapid.løst())
    }

    private fun TestRapid.løst() = inspektør.message(0)
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