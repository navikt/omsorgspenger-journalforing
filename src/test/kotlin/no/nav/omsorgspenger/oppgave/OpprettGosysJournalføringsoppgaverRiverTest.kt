package no.nav.omsorgspenger.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import java.util.*
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.printSisteMelding
import no.nav.omsorgspenger.testutils.wiremock.HentJournalpostId1
import no.nav.omsorgspenger.testutils.wiremock.HentJournalpostId2
import no.nav.omsorgspenger.testutils.wiremock.HentOppgaveId1
import no.nav.omsorgspenger.testutils.wiremock.OpprettJournalpostId1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class OpprettGosysJournalføringsoppgaverRiverTest(
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
            identitetsnummer = "11111111111",
            journalpostIder = setOf(HentJournalpostId1, HentJournalpostId2)
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.mockLøsningPåHentePersonopplysninger("11111111111")

        Assertions.assertEquals(2, rapid.inspektør.size)
        Assertions.assertTrue(rapid.inspektør.message(1).get("@løsninger").toString().contains(HentOppgaveId1))
    }

    @Test
    fun `Behov med två journalpostId varav en existerande`() {
        val (_, behovssekvens) = nyBehovsSekvens(
            id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
            identitetsnummer = "11111111111",
            journalpostIder = setOf(HentJournalpostId1, OpprettJournalpostId1)
        )

        rapid.sendTestMessage(behovssekvens)
        rapid.mockLøsningPåHentePersonopplysninger("11111111111")
        rapid.printSisteMelding()

        Assertions.assertEquals(2, rapid.inspektør.size)
        Assertions.assertTrue(rapid.inspektør.message(1).get("@løsninger").toString().contains(HentOppgaveId1))
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
                        "berørteIdentitetsnummer" to setOf("11111111112", "11111111113")
                    ),
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

private fun String.somJsonMessage() =
    JsonMessage(toString(), MessageProblems(this), null).also { it.interestedIn("@løsninger") }

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger(identitetsnummer: String) = leggTilLøsning(
    behov = "HentPersonopplysninger@opprettGosysJournalføringsoppgaver",
    løsning = mapOf(
        "personopplysninger" to mapOf(
            identitetsnummer to mapOf(
                "aktørId" to "9$identitetsnummer",
            )
        ),
        "fellesopplysninger" to mapOf(
            "enhetsnummer" to "4487"
        )
    )
)