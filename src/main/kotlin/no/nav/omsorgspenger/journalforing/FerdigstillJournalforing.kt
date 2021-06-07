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
import no.nav.omsorgspenger.Fagsystem
import org.slf4j.LoggerFactory

internal abstract class FerdigstillJournalforing(
    rapidsConnection: RapidsConnection,
    private val journalforingMediator: JournalforingMediator,
    private val behov: String,
    private val fagsystem: Fagsystem) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(FerdigstillJournalforing::class.java)) {

    private val JOURNALPOSTIDER = "@behov.$behov.journalpostIder"
    private val IDENTITETSNUMMER = "@behov.$behov.identitetsnummer"
    private val SAKSNUMMER = "@behov.$behov.saksnummer"

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(behov)
                packet.require(JOURNALPOSTIDER) { it.requireArray { entry -> entry is TextNode } }
                packet.require(IDENTITETSNUMMER, JsonNode::asText)
                packet.require(SAKSNUMMER, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $behov")

        val journalpostIder = packet[JOURNALPOSTIDER]
            .map { it.asText() }
            .toSet()
        val identitetsnummer = packet[IDENTITETSNUMMER].asText()
        val saksnummer = packet[SAKSNUMMER].asText()

        logger.info("Saksnummer: $saksnummer, JournalpostIder: $journalpostIder")

        journalpostIder.forEach {
            journalforingMediator.behandlaJournalpost(
                correlationId = packet["@correlationId"].asText(),
                journalpost = Journalpost(
                    journalpostId = it,
                    identitetsnummer = identitetsnummer,
                    saksnummer = saksnummer,
                    fagsaksystem = fagsystem
                )
            ).let { success -> if (!success) {
                return false
            }}
        }
        packet.leggTilLøsning(behov)
        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $behov")
    }
}