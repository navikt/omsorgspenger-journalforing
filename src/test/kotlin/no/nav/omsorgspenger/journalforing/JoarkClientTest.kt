package no.nav.omsorgspenger.journalforing

import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.JournalpostStatus
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class JoarkClientTest(
        applicationContext: ApplicationContext) {

    private val client = applicationContext.joarkClient

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
                    fagsaksystem = "OMSORGSPENGER"
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
                    fagsaksystem = "OMSORGSPENGER"
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
                    fagsaksystem = "OMSORGSPENGER"
                )
            )
        }

        assertEquals(JournalpostStatus.Ferdigstilt, result)
    }

}