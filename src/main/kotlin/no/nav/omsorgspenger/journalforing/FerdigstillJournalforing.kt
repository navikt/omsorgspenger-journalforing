package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import org.slf4j.LoggerFactory

internal class FerdigstillJournalforing(
        rapidsConnection: RapidsConnection,
        private val journalforingMediator: JournalforingMediator) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(this::class.java)
) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.require(JOURNALPOSTIDER, JsonNode::asText)
                packet.require(IDENTITETSNUMMER, JsonNode::asText)
                packet.require(SAKSNUMMER, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $BEHOV med id $id").also { incMottattBehov() }

        val journalpostIder = packet[JOURNALPOSTIDER].toSet()
        val identitetsnummer = packet[IDENTITETSNUMMER].asText()
        val saksnummer = packet[SAKSNUMMER].asText()

        journalpostIder.forEach {
            journalforingMediator.behandlaJournalpost(
                    correlationId = packet["@correlationId"].asText(),
                    journalpostPayload = JournalpostPayload(
                            journalpostId = it.asText(),
                            bruker = JournalpostPayload.Bruker(id = identitetsnummer),
                            sak = JournalpostPayload.Sak(fagsakId = saksnummer)
                    )
            ).let { success ->
                if(success) {
                    packet.leggTilLøsning(BEHOV, mapOf("løsning" to packet["@behov.$BEHOV.saksnummer"]))
                }
                else {
                    logger.error("Feil vid behandling av behov: $id").also { incBehandlingFeil() }
                    return false
                }
            }
        }

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