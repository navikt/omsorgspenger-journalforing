package no.nav.omsorgspenger.journalforing

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.JoarkClient
import org.slf4j.LoggerFactory

class JournalforingMediator(
        private val joarkClient: JoarkClient
) {

    internal val logger = LoggerFactory.getLogger(this::class.java)

    internal fun behandlaJournalpost(correlationId: String, journalpostPayload: JournalpostPayload): Boolean {
        var result = false

        // TODO: Hantera = uppdatering av post som redan är uppdaterat men inte färdigställt?
        runBlocking {
            val journalpostId = journalpostPayload.journalpostId
            joarkClient.oppdaterJournalpost(
                    hendelseId = correlationId,
                    journalpostPayload = journalpostPayload
            ).let { success ->
                if (success) {
                    logger.info("Oppdatert journalpostid: $journalpostId")
                    joarkClient.ferdigstillJournalpost(
                            hendelseId = correlationId,
                            journalpostPayload
                    ).let { success ->
                        if (success) {
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

}