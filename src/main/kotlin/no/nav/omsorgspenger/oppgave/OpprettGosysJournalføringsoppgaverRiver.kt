package no.nav.omsorgspenger.oppgave

import io.prometheus.client.Counter
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
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
        logger.info("Skal løse behov ${OpprettGosysJournalføringsoppgaverMelding.behovNavn}.")

        if (behov.manglerPersonopplysninger) {
            logger.info("Legger til behov for å hente personopplysninger.")
            OpprettGosysJournalføringsoppgaverMelding.leggTilBehovForPersonopplysninger(
                packet = packet
            )
            return true
        }

        logger.info("Henter eksisterende oppgaver for ${behov.journalpostIder.size} journalpostIder")
        var løsning = runBlocking { oppgaveClient.hentOppgave(
            aktørId = behov.aktørId!!,
            journalpostIder = behov.journalpostIder,
            correlationId = correlationId
        )}.filterKeys { behov.journalpostIder.contains(it.somJournalpostId()) }

        if (løsning.isNotEmpty()) {
            logger.info("JournalpostIder=${løsning.keys} har allerede journalføringsoppgave.")
        }

        behov.journalpostIder.filterNot {
            løsning.keys.contains("$it")
        }.forEach { journalpostId ->
            val result = runBlocking { oppgaveClient.opprettOppgave(
                oppgave = Oppgave(
                    journalpostType = behov.journalpostType,
                    journalpostId = "$journalpostId",
                    aktørId = "${behov.aktørId}",
                    enhetsNummer = "${behov.enhetsnummer}"
                ),
                correlationId = correlationId
            )}
            logger.info("Opprettet journalføringsoppgave for journalpostId=$journalpostId")
            løsning = løsning.plus(result)
        }

        require(løsning.keys.containsAll(behov.journalpostIder.map { "$it" })) {
            "Klarte ikke å opprette oppgave for alle journalpostIdene."
        }

        packet.leggTilLøsning(OpprettGosysJournalføringsoppgaverMelding.behovNavn, mapOf(
            "oppgaveIder" to løsning
        ))

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