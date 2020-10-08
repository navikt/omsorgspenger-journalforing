package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireArray
import no.nav.k9.rapid.river.skalLøseBehov
import org.slf4j.LoggerFactory

internal class FerdigstillJournalforing(
        rapidsConnection: RapidsConnection,
        private val journalforingMediator: JournalforingMediator) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(FerdigstillJournalforing::class.java)
) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.require(JOURNALPOSTIDER) { it.requireArray { entry -> entry is TextNode } }
                packet.require(IDENTITETSNUMMER, JsonNode::asText)
                packet.require(SAKSNUMMER, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $BEHOV med id $id").also { incMottattBehov() }

        val journalpostIder = packet[JOURNALPOSTIDER]
                .map { it.asText() }
                .toSet()
        val identitetsnummer = packet[IDENTITETSNUMMER].asText()
        val saksnummer = packet[SAKSNUMMER].asText()

        journalpostIder.forEach {
            journalforingMediator.behandlaJournalpost(
                    correlationId = packet["@correlationId"].asText(),
                    journalpostPayload = JournalpostPayload(
                            journalpostId = it,
                            bruker = JournalpostPayload.Bruker(id = identitetsnummer),
                            sak = JournalpostPayload.Sak(fagsakId = saksnummer)
                    )
            ).let { success ->
                if (success) logger.info("Behandlet journalpostId: $it")
                else {
                    logger.error("Feil vid behandling av journalpostId: $it").also { incBehandlingFeil() }
                    return false
                }
            }
        }
        packet.leggTilLøsning(BEHOV)
        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV med id $id").also { incBehandlingUtfort() }
    }

    internal companion object {
        const val BEHOV = "FerdigstillJournalføringForOmsorgspenger"
        const val JOURNALPOSTIDER = "@behov.$BEHOV.journalpostIder"
        const val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
        const val SAKSNUMMER = "@behov.$BEHOV.saksnummer"
    }
}