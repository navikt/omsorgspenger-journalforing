package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.journalforing.JournalpostManglerNavn.behandlaJournalpostHåndterManglerNavn
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
                packet.interestedIn(JournalpostManglerNavn.PersonopplysningerKey)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $behov")

        val journalpostIder = packet[JOURNALPOSTIDER]
            .map { it.asText().somJournalpostId() }
            .toSet()
        val identitetsnummer = packet[IDENTITETSNUMMER].asText().somIdentitetsnummer()
        val saksnummer = packet[SAKSNUMMER].asText().somSaksnummer()

        logger.info("Saksnummer: $saksnummer, JournalpostIder: $journalpostIder")

        return journalforingMediator.behandlaJournalpostHåndterManglerNavn(
            packet = packet,
            aktueltBehov = behov,
            identitetsnummer = identitetsnummer,
            saksnummer = saksnummer,
            fagsystem = fagsystem,
            journalpostIder = journalpostIder,
            onOk = {
                packet.leggTilLøsning(behov)
                logger.info("Løst behov $behov")
                true
            }
        )
    }
}