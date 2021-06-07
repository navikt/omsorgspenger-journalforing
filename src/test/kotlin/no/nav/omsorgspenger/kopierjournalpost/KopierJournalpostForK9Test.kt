package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class KopierJournalpostForK9Test(
    private val applicationContext: ApplicationContext) {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `kopierer journalpost for K9`() {
        val (_, behovssekvens) = nyBehovsSekvens(
            id = "01F7JZ03VCNG2C0JRGVM6KTZHC"
        )

        rapid.sendTestMessage(behovssekvens)
        val sisteMelding = rapid.sisteMelding()
        val kopiertJournalpostId = sisteMelding.getJSONObject("@løsninger").getJSONObject(BEHOV).getString("journalpostId")
        assertEquals("1234", kopiertJournalpostId)
    }

    private companion object {
        const val BEHOV = "KopierJournalpostForK9"

        private fun TestRapid.sisteMelding() = JSONObject(inspektør.message(inspektør.size - 1).toString())

        private fun nyBehovsSekvens(
            id: String,
            correlationId: String = UUID.randomUUID().toString()
        ) = Behovssekvens(
            id = id,
            correlationId = correlationId,
            behov = arrayOf(
                Behov(
                    navn = BEHOV,
                    input = mapOf(
                        "versjon" to "1.0.0",
                        "journalpostId" to "1111",
                        "fra" to mapOf(
                            "identitetsnummer" to "11111111111",
                            "saksnummer" to "SAK1"
                        ),
                        "til" to mapOf(
                            "identitetsnummer" to "22222222222",
                            "saksnummer" to "SAK2"
                        )
                    )
                )
            )
        ).keyValue
    }
}