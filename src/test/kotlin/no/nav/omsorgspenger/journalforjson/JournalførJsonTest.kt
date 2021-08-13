package no.nav.omsorgspenger.journalforjson

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class JournalførJsonTest(
    private val applicationContext: ApplicationContext) {

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContext)
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
    }

    @Test
    fun `Opprette journalpost for json`() {
        val (_, behovsskevens) = nyBehovsSekvens()

        rapid.sendTestMessage(behovsskevens)

        val journalpostId = rapid.journalpostId()

        assertEquals("12345678".somJournalpostId(), journalpostId)
    }

    @Test
    fun `Opprette journalpost for json med suffixed behov`() {
        val behovSuffix = "punsjOppsummering"
        val (_, behovsskevens) = nyBehovsSekvens(
            correlationId = "allerede-opprettet",
            behovSuffix = behovSuffix
        )

        rapid.sendTestMessage(behovsskevens)

        val journalpostId = rapid.journalpostId(
            behovSuffix = behovSuffix
        )

        assertEquals("910111213".somJournalpostId(), journalpostId)
    }

    internal companion object {
        const val BEHOV = "JournalførJson"

        private fun String?.behovNavn() = when (this) {
            null -> BEHOV
            else -> "$BEHOV@$this"
        }

        private fun TestRapid.journalpostId(behovSuffix: String? = null) = inspektør.message(0)
            .get("@løsninger")
            .get(behovSuffix.behovNavn())
            .get("journalpostId")
            .asText()
            .somJournalpostId()

        private fun nyBehovsSekvens(
            id: String = ULID().nextULID(),
            correlationId: String = UUID.randomUUID().toString(),
            behovSuffix: String? = null
        ) = Behovssekvens(
            id = id,
            correlationId = correlationId,
            behov = arrayOf(
                Behov(
                    navn = behovSuffix.behovNavn(),
                    input = mapOf(
                        "json" to mapOf(
                            "foo" to true
                        ),
                        "identitetsnummer" to "11111111111",
                        "fagsystem" to "K9",
                        "saksnummer" to "ABC123",
                        "brevkode" to "K9_PUNSJ_OPPSUMMERING",
                        "mottatt" to "2021-05-03T16:08:45.800Z",
                        "farge" to "#00ff00",
                        "tittel" to "Oppsummering fra punsj",
                        "avsender" to mapOf(
                            "navn" to "\"Saks behandlersen\""
                        )
                    )
                )
            )
        ).keyValue
    }
}