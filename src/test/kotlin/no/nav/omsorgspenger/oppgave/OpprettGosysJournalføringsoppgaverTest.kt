package no.nav.omsorgspenger.oppgave

import java.util.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class OpprettGosysJournalføringsoppgaverTest(
        private val applicationContext: ApplicationContext) {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Henter existerande oppgaver`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                identitetsnummer = "1111111111",
                journalpostIder = setOf("HentJournalpostId1", "HentJournalpostId2")
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.mockLøsningPåHentePersonopplysninger("1111111111")

        Assertions.assertEquals(2, rapid.inspektør.size)
        Assertions.assertTrue(rapid.inspektør.message(1).get("@løsninger").toString().contains("HentOppgaveId1"))
    }

    @Test
    fun `Behov med två journalpostId varav en existerande`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                identitetsnummer = "1111111111",
                journalpostIder = setOf("HentJournalpostId1", "OpprettJournalpostId1")
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.mockLøsningPåHentePersonopplysninger("1111111111")

        Assertions.assertEquals(2, rapid.inspektør.size)
        Assertions.assertTrue(rapid.inspektør.message(1).get("@løsninger").toString().contains("HentOppgaveId1"))
        Assertions.assertEquals(2, rapid.inspektør.message(1)["@løsninger"][BEHOV]["oppgaveIder"].size())
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"

        private fun nyBehovsSekvens(
                id: String,
                identitetsnummer: String,
                journalpostIder: Set<String>,
                correlationId: String = UUID.randomUUID().toString()
        ) = Behovssekvens(
                id = id,
                correlationId = correlationId,
                behov = arrayOf(
                        Behov(
                                navn = BEHOV,
                                input = mapOf(
                                        "identitetsnummer" to identitetsnummer,
                                        "journalpostType" to "OverføreOmsorgsdager",
                                        "journalpostIder" to journalpostIder,
                                        "berørteIdentitetsnummer" to setOf("123","456")
                                )
                        )
                )
        ).keyValue
    }

}

internal fun TestRapid.mockLøsningPåHentePersonopplysninger(identitetsnummer: String) {
    sendTestMessage(
            sisteMelding()
                    .somJsonMessage()
                    .leggTilLøsningPåHentePersonopplysninger(identitetsnummer)
                    .toJson()
    )
}

private fun TestRapid.sisteMelding() = inspektør.message(inspektør.size - 1).toString()

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger(identitetsnummer: String) = leggTilLøsning(
        behov = "HentPersonopplysninger",
        løsning = mapOf(
                "personopplysninger" to mapOf(
                        identitetsnummer to mapOf(
                                "aktørId" to "11111",
                        )
                )
        )
)