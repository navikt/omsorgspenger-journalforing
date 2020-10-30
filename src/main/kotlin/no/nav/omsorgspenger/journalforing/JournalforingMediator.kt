package no.nav.omsorgspenger.journalforing

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.JournalpostStatus
import org.slf4j.LoggerFactory

internal class JournalforingMediator(
        private val joarkClient: JoarkClient) {

    internal fun behandlaJournalpost(
        correlationId: String,
        journalpost: Journalpost): Boolean {
        var result = false
        val journalpostId = journalpost.journalpostId
        logger.info("Behandler journalpost $journalpostId for saksnummer ${journalpost.saksnummer}")

        // TODO: Håndtere når det går bra for en journalpost, men ikke alle..

        runBlocking {
            joarkClient.oppdaterJournalpost(
                    correlationId = correlationId,
                    journalpost = journalpost
            ).let { statusEtterOppdatering ->
                logger.info("Status etter oppdatering: $statusEtterOppdatering")
                when (statusEtterOppdatering) {
                    JournalpostStatus.Oppdatert -> {
                        joarkClient.ferdigstillJournalpost(
                            correlationId = correlationId,
                            journalpostId = journalpostId
                        ).let { statusEtterFerdigstilling ->
                            logger.info("Status etter ferdigstilling: $statusEtterFerdigstilling")
                            when (statusEtterFerdigstilling) {
                                JournalpostStatus.Ferdigstilt -> {
                                    result = true
                                }
                                else -> {
                                    logger.error("Feil ved ferdigstilling av journalpost.")
                                }
                            }
                        }
                    }
                    JournalpostStatus.Ferdigstilt -> {
                        logger.warn("Journalpost allerede ferdigstilt. Bør sjekkes om den er ferdigstilt mot rett saksnummer!")
                        result = true
                    }
                    else -> {
                        logger.error("Feil ved oppdatering av journalpost.")
                    }
                }
            }
        }.also {
            return result
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(JournalforingMediator::class.java)
    }
}