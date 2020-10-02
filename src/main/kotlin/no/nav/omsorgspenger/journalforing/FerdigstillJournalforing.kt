package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.requireText
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.JoarkClient
import no.nav.omsorgspenger.JournalpostPayload
import org.slf4j.LoggerFactory
import java.util.*

internal class FerdigstillJournalforing(
        rapidsConnection: RapidsConnection,
        joarkClient: JoarkClient) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val joarkClient = joarkClient

    init {
        River(rapidsConnection).apply {
            validate {
                it.skalLøseBehov(BEHOV)
                it.require(IDENTITETSNUMMER, JsonNode::requireText)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        incMottattBehov()
        val id = packet["@id"].asText()
        logger.info("Skal løse behov $BEHOV med id $id")

        runBlocking {
            val journalPayload = JournalpostPayload(
                    bruker = JournalpostPayload.Bruker(packet[IDENTITETSNUMMER].asText()),
                    journalpostId = "",
                    sak = JournalpostPayload.Sak(fagsakId = packet["fagsak"].asText()),
                    tittel = ""
            )

            joarkClient.oppdaterJournalpost(
                    hendelseId = UUID.fromString(JOURNALPOSTID),
                    journalpostPayload = journalPayload
            ).let { success ->
                if(success) logger.info("Success!")
                else logger.warn("NOT success!")
            }
        }


    }

    internal companion object {
        const val BEHOV = "FerdigstillJournalføringForOmsorgspenger"
        const val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
        const val JOURNALPOSTID = "@behov.$BEHOV.journalpostid"
    }
}