package no.nav.omsorgspenger.joark

import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.CorrelationId.Companion.somCorrelationId
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