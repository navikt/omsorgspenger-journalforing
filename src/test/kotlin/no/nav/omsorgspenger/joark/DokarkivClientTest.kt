package no.nav.omsorgspenger.joark

import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.CorrelationId.Companion.somCorrelationId
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class DokarkivClientTest(
        applicationContext: ApplicationContext) {

    private val client = applicationContext.dokarkivClient

    @Test
    fun `ferdigstill journalpost`() {

        val correlationId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.ferdigstillJournalpost(
                correlationId = correlationId,
                journalpostId = "123"
            )
        }

        assertEquals(JournalpostStatus.Ferdigstilt, result)
    }

    @Test
    fun `oppdater journalpost`() {

        val correlationId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.oppdaterJournalpost(
                correlationId = correlationId,
                journalpost = Journalpost(
                    journalpostId = "123",
                    identitetsnummer = "12312312311",
                    saksnummer = "123",
                    fagsaksystem = Fagsystem.OMSORGSPENGER
                )
            )
        }
        assertEquals(JournalpostStatus.Oppdatert, result)
    }

    @Test
    fun `Uventet feil ved oppdater journalpost`() {
        val result = runBlocking {
            client.oppdaterJournalpost(
                correlationId = "400",
                journalpost = Journalpost(
                    journalpostId = "400",
                    identitetsnummer = "12312312311",
                    saksnummer = "123",
                    fagsaksystem = Fagsystem.OMSORGSPENGER
                )
            )
        }

        assertEquals(JournalpostStatus.Feilet, result)
    }

    @Test
    fun `Uventet feil ved ferdigstill journalpost`() {
        val result = runBlocking {
            client.ferdigstillJournalpost(
                correlationId = "feil-ved-ferdigstilling",
                journalpostId = "123123"
            )
        }

        assertEquals(JournalpostStatus.Feilet, result)
    }

    @Test
    fun `Allerede ferdigstilt ved oppdatering av journalpost`() {
        val result = runBlocking {
            client.oppdaterJournalpost(
                correlationId = "allerede-ferdigstilt",
                journalpost = Journalpost(
                    journalpostId = "123123",
                    identitetsnummer = "12312312311",
                    saksnummer = "123",
                    fagsaksystem = Fagsystem.OMSORGSPENGER
                )
            )
        }

        assertEquals(JournalpostStatus.Ferdigstilt, result)
    }

    @Test
    fun `opprette journalpost`() {
        val journalpostId = runBlocking { client.opprettJournalpost(
            correlationId = "${UUID.randomUUID()}".somCorrelationId(),
            nyJournalpost = NyJournalpostTest.nyJournalpost
        )}

        assertEquals("12345678".somJournalpostId(), journalpostId)
    }

    @Test
    fun `journalpost allerede opprettet`() {
        val journalpostId = runBlocking { client.opprettJournalpost(
            correlationId = "allerede-opprettet".somCorrelationId(),
            nyJournalpost = NyJournalpostTest.nyJournalpost
        )}

        assertEquals("910111213".somJournalpostId(), journalpostId)
    }

    @Test
    fun `opprettet journalpost ikke ferdigstilt`() {
        assertThrows<IllegalStateException> {
            runBlocking { client.opprettJournalpost(
                correlationId = "ikke-ferdigstilt".somCorrelationId(),
                nyJournalpost = NyJournalpostTest.nyJournalpost
            )}
        }
    }
}