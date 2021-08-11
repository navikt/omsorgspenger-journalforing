package no.nav.omsorgspenger.ferdigstilljournalforing

import kotlinx.coroutines.runBlocking
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.JournalpostStatus
import org.slf4j.LoggerFactory

internal class FerdigstillJournalføringMediator(
        private val dokarkivClient: DokarkivClient
) {

    internal fun behandlaJournalpost(
        correlationId: String,
        journalpost: Journalpost
    ): Boolean {
        var result = false
        val journalpostId = journalpost.journalpostId

        runBlocking {
            dokarkivClient.oppdaterJournalpost(
                    correlationId = correlationId,
                    journalpost = journalpost
            ).let { statusEtterOppdatering ->
                logger.info(journalpost.log("Status etter oppdatering: $statusEtterOppdatering"))
                when (statusEtterOppdatering) {
                    JournalpostStatus.Oppdatert -> {
                        dokarkivClient.ferdigstillJournalpost(
                            correlationId = correlationId,
                            journalpostId = journalpostId
                        ).let { statusEtterFerdigstilling ->
                            logger.info(journalpost.log("Status etter ferdigstilling: $statusEtterFerdigstilling"))
                            when (statusEtterFerdigstilling) {
                                JournalpostStatus.Ferdigstilt -> {
                                    result = true
                                }
                                else -> {
                                    logger.error(journalpost.log("Feil ved ferdigstilling"))
                                }
                            }
                        }
                    }
                    JournalpostStatus.Ferdigstilt -> {
                        logger.warn(journalpost.log("Allerede ferdigstilt. Bør sjekkes om den er ferdigstilt mot rett saksnummer!"))
                        result = true
                    }
                    else -> {
                        logger.error(journalpost.log("Feil ved oppdatering"))
                    }
                }
            }
        }.also {
            return result
        }
    }

    private fun Journalpost.log(log: String) = "[JournalpostId=$journalpostId] $log"

    private companion object {
        private val logger = LoggerFactory.getLogger(FerdigstillJournalføringMediator::class.java)
    }
}