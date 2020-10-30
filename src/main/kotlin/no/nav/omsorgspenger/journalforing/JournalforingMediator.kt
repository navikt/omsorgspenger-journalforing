package no.nav.omsorgspenger.journalforing

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.JoarkClient
import org.slf4j.LoggerFactory

internal class JournalforingMediator(
        private val joarkClient: JoarkClient) {

    internal fun behandlaJournalpost(correlationId: String, journalpost: Journalpost): Boolean {
        var result = false

        // TODO: Hantera = uppdatering av post som redan är uppdaterat men inte färdigställt?
        runBlocking {
            val journalpostId = journalpost.journalpostId
            joarkClient.oppdaterJournalpost(
                    correlationId = correlationId,
                    journalpost = journalpost
            ).let { oppdaterJournalpostSuccess ->
                if (oppdaterJournalpostSuccess) {
                    logger.info("Oppdatert journalpostid: $journalpostId")
                    joarkClient.ferdigstillJournalpost(
                            correlationId = correlationId,
                            journalpostId = journalpostId
                    ).let { ferdigstiltJournalpostSuccess ->
                        if (ferdigstiltJournalpostSuccess) {
                            result = true
                            logger.info("Ferdigstillt journalpostid: $journalpostId")
                        }
                        else logger.warn("Feil vid ferdigstilling av journalpostid: $journalpostId")
                    }
                }
                else logger.warn("Feil vid oppdatering av journalpostid: $journalpostId")
            }
        }.also {
            return result
        }

    }

    private companion object {
        private val logger = LoggerFactory.getLogger(JournalforingMediator::class.java)
    }
}