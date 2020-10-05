package no.nav.omsorgspenger.journalforing

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.nav.omsorgspenger.JoarkClient
import org.slf4j.LoggerFactory
import kotlin.coroutines.EmptyCoroutineContext

class JournalforingMediator(
        private val joarkClient: JoarkClient
) {

    internal val logger = LoggerFactory.getLogger(this::class.java)

    internal fun behandlaJournalpost(behovPayload: BehovPayload): Boolean {
        val journalpostId = behovPayload.journalpostId
        var result = false

        runBlocking {
            joarkClient.oppdaterJournalpost(
                    hendelseId = behovPayload.hendelseId,
                    behovPayload = behovPayload
            ).let { success ->
                if (success) {
                    logger.info("Oppdatert journalpostid: $journalpostId")
                    joarkClient.ferdigstillJournalpost(
                            hendelseId = behovPayload.hendelseId,
                            behovPayload
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