package no.nav.omsorgspenger.journalforing

import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.ZonedDateTime
import kotlin.test.assertNotNull

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
    fun `Godtar requests med flera journalpostIder`() {
        val (_, behovssekvens) = nyBehovsSekvens(
                id = "01BX5ZZKBKACTAV9WEVGEMMVS0",
                journalpostIder = setOf("123abc", "345def")
        )
        rapid.sendTestMessage(behovssekvens)

        assertEquals(1, rapid.inspektør.size)
        val løst = rapid.inspektør.message(0)
            .get("@løsninger")
            .get(BEHOV)
            .get("løst")
            .asText()
            .let { ZonedDateTime.parse(it) }

        assertNotNull(løst)
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