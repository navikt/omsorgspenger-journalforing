package no.nav.omsorgspenger.journalforing

import java.util.UUID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime

@ExtendWith(ApplicationContextExtension::class)
internal class FerdigstillJournalforingTest(
        private val applicationContext: ApplicationContext) {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Håndterer flere journalpostIder`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                journalpostIder = setOf("12345", "678910")
        )
        rapid.sendTestMessage(behovssekvens)

        assertNotNull(rapid.løst())
    }

    @Test
    fun `Håndterer om journalpost allerede er ferdigstilt`() {
        val (_, behovssekvens) = nyBehovsSekvens(
            id = "01ENX3XB5S98AMKRX2JV638YNN",
            journalpostIder = setOf("12345"),
            correlationId = "allerede-ferdigstilt"
        )
        rapid.sendTestMessage(behovssekvens)
        assertNotNull(rapid.løst())
    }

    @Test
    fun `Håndterer tom list med journalposter`() {
        val (_, behovssekvens) = nyBehovsSekvens(
            id = "01ENX4ABV5CSAV3FTBZVVGV2HN",
            journalpostIder = setOf()
        )
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