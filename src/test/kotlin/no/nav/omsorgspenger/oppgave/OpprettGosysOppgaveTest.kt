package no.nav.omsorgspenger.oppgave

import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertNotNull
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
internal class OpprettGosysOppgaveTest(
        private val applicationContext: ApplicationContext) {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Plockar upp och sender vidare behov før aktørid-opplysninger`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                identitetsnummer = "1111111111"
        )

        rapid.sendTestMessage(behovssekvens)

        rapid.mockLøsningPåHentePersonopplysninger()

        Assertions.assertEquals(2, rapid.inspektør.size)
        Assertions.assertTrue(rapid.inspektør.message(0).toString().contains("løsninger"))
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"

        private fun nyBehovsSekvens(
                id: String,
                identitetsnummer: String,
                correlationId: String = UUID.randomUUID().toString()
        ) = Behovssekvens(
                id = id,
                correlationId = correlationId,
                behov = arrayOf(
                        Behov(
                                navn = BEHOV,
                                input = mapOf(
                                        "identitetsnummer" to identitetsnummer,
                                        "journalpostType" to "testType",
                                        "journalpostIder" to setOf("123123", "456456"),
                                        "berørteIdentitetsnummer" to setOf("123","456")
                                )
                        )
                )
        ).keyValue
    }

}

internal fun TestRapid.mockLøsningPåHentePersonopplysninger() {
    sendTestMessage(
            sisteMelding()
                    .somJsonMessage()
                    .leggTilLøsningPåHentePersonopplysninger()
                    .toJson()
    )
}

private fun TestRapid.sisteMelding() = inspektør.message(inspektør.size - 1).toString()

private fun String.somJsonMessage() = JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }

private fun JsonMessage.leggTilLøsningPåHentePersonopplysninger() = leggTilLøsning(
        behov = "HentPersonopplysninger",
        løsning = mapOf(
                "personopplysninger" to mapOf(
                        "attributer" to mapOf(
                                "aktørId" to "11111",
                        )
                )
        )
)