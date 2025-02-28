package no.nav.omsorgspenger.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.extensions.PrometheusExt.ensureRegistered
import org.slf4j.LoggerFactory

internal class OpprettGosysJournalføringsoppgaverRiver(
    rapidsConnection: RapidsConnection,
    private val oppgaveClient: OppgaveClient) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(OpprettGosysJournalføringsoppgaverRiver::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(OpprettGosysJournalføringsoppgaverMelding.behovNavn)?.also {
                    OpprettGosysJournalføringsoppgaverMelding.validateBehov(packet)
                }
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val behov = OpprettGosysJournalføringsoppgaverMelding.hentBehov(packet)
        val correlationId = packet.correlationId()
        logger.info("Skal løse behov ${OpprettGosysJournalføringsoppgaverMelding.behovNavn} for journalpostIder=${behov.journalpostIder}.")

        if (behov.manglerPersonopplysninger) {
            logger.info("Legger til behov for å hente personopplysninger.")
            OpprettGosysJournalføringsoppgaverMelding.leggTilBehovForPersonopplysninger(
                packet = packet
            )
            return true
        }

        var journalføringsoppgaver = runBlocking { oppgaveClient.hentJournalføringsoppgaver(
            aktørId = behov.aktørId!!,
            journalpostIder = behov.journalpostIder,
            correlationId = correlationId
        )}.filterKeys { behov.journalpostIder.contains(it) }

        if (journalføringsoppgaver.isNotEmpty()) {
            logger.info("JournalpostIder=${journalføringsoppgaver.keys} har allerede tilhørende journalføringsoppgaver.")
        }

        behov.journalpostIder.filterNot {
            journalføringsoppgaver.keys.contains(it)
        }.forEach { journalpostId ->
            val oppgaveId = runBlocking { oppgaveClient.opprettJournalføringsoppgave(
                oppgave = Oppgave(
                    journalpostType = behov.journalpostType,
                    journalpostId = journalpostId,
                    aktørId = behov.aktørId!!,
                    enhetsNummer = behov.enhetsnummer!!
                ),
                correlationId = correlationId
            )}
            journalføringsoppgaver = journalføringsoppgaver.plus(journalpostId to oppgaveId)
        }

        require(journalføringsoppgaver.keys.containsAll(behov.journalpostIder)) {
            "Har kun funnet/opprettet journalføringsoppgaver for journalpostIder=${journalføringsoppgaver.keys}"
        }

        OpprettGosysJournalføringsoppgaverMelding.leggTilLøsning(
            packet = packet,
            løsning = journalføringsoppgaver
        )

        logger.info("JournalpostId/OppgaveId-mapping=$journalføringsoppgaver")
        gosysJournalforingsoppgaveCounter.labels(behov.journalpostType).inc(behov.journalpostIder.size.toDouble())
        return true
    }


    private companion object {
        private val gosysJournalforingsoppgaveCounter = Counter
            .build("gosysJournalforingsoppgave", "Antall Gosys journalføringsoppgaver opprettet per journalposttype")
            .labelNames("journalpostType")
            .create()
            .ensureRegistered()
    }
}