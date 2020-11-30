package no.nav.omsorgspenger.oppgave

import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforingTest
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class InitierGosysJournalføringsoppgaverTest(
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

        assertNotNull(rapid.inspektør.message(0))
    }
    private fun TestRapid.løst() = inspektør.message(0)
            .get("@løsninger")
            .get(BEHOV)
            .get("løst")
            .asText()
            .let { ZonedDateTime.parse(it) }


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
                                        "berørteIdentitetsnummer" to setOf("123","456")
                                )
                        )
                )
        ).keyValue
    }

}
