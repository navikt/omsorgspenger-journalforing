package no.nav.omsorgspenger.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.CorrelationId.Companion.somCorrelationId
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApplicationContextExtension::class)
internal class OppgaveClientTest(
    applicationContext: ApplicationContext) {

    private val client = applicationContext.oppgaveClient

    @Test
    fun `Accepterer inte ustøttet journalpostType`() {

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                client.opprettOppgave(
                    correlationId = "testtesttest".somCorrelationId(),
                    oppgave = Oppgave(
                        journalpostType = "feiltype",
                        journalpostId = "test",
                        aktørId = "test",
                        enhetsNummer = "test"
                    )
                )
            }
        }
    }
}