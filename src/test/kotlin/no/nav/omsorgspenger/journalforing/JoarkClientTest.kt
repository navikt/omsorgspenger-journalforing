package no.nav.omsorgspenger.journalforing

import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class JoarkClientTest(
        applicationContext: ApplicationContext) {

    private val client = applicationContext.joarkClient

    @Test
    fun `ferdigstill journalpost test`() {

        val hendelseId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.ferdigstillJournalpost(
                    correlationId = hendelseId,
                    journalpostPayload = JournalpostPayload(
                            journalpostId = "123",
                            bruker = JournalpostPayload.Bruker(id = "12312312311"),
                            sak = JournalpostPayload.Sak(fagsakId = "123")
                    )
            )
        }

        assertTrue(result)
    }

    @Test
    fun `oppdater journalpost test`() {

        val hendelseId = UUID.randomUUID().toString()

        val result = runBlocking {
            client.oppdaterJournalpost(
                    correlationId = hendelseId,
                    journalpostPayload = JournalpostPayload(
                            journalpostId = "123",
                            bruker = JournalpostPayload.Bruker(id = "12312312311"),
                            sak = JournalpostPayload.Sak(fagsakId = "123")
                    )
            )
        }

        assertTrue(result)
    }

    @Test
    fun `test 400`() {

        val result = runBlocking {
            client.oppdaterJournalpost(
                    correlationId = "400",
                    journalpostPayload = JournalpostPayload(
                            journalpostId = "400",
                            bruker = JournalpostPayload.Bruker(id = "12312312311"),
                            sak = JournalpostPayload.Sak(fagsakId = "123"),
                    )
            )
        }

        assertFalse(result)
    }

}