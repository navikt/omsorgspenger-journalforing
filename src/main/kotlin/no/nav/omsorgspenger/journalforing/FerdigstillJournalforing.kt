package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireText
import no.nav.k9.rapid.river.sendMedId
import no.nav.k9.rapid.river.skalLøseBehov
import org.slf4j.LoggerFactory

internal class FerdigstillJournalforing(
        rapidsConnection: RapidsConnection,
        journalforingMediator: JournalforingMediator) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val journalforingMediator = journalforingMediator

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(BEHOV)
                it.require(JOURNALPOSTID, JsonNode::requireText)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        incMottattBehov()
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $BEHOV med id $id")

        journalforingMediator.behandlaJournalpost(
                behovPayload = BehovPayload(packet) // Parse packet -> Payload
        ).let { success ->
            if(success) {
                packet.leggTilLøsning(BEHOV, mapOf("løsning" to "journalpost ferdigstillt!"))
                logger.info("Løst behov $BEHOV med id $id")
                context.sendMedId(packet)
                incBehandlingUtfort()
            }
            else {
                logger.error("Feil vid behandling av behov: $id")
                incBehandlingFeil()
            }
        }

    }

    internal companion object {
        const val BEHOV = "FerdigstillJournalføringForOmsorgspenger"
        const val JOURNALPOSTID = "@behov.$BEHOV.journalpostid"
    }
}