package no.nav.omsorgspenger.kopierjournalpost

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.DokarkivproxyClient
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.journalforing.JournalforingMediator
import no.nav.omsorgspenger.journalforing.Journalpost
import org.slf4j.LoggerFactory

internal abstract class KopierJournalpost(
    rapidsConnection: RapidsConnection,
    private val journalforingMediator: JournalforingMediator,
    private val dokarkivproxyClient: DokarkivproxyClient,
    private val fagsystem: Fagsystem,
    private val behov: String
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierJournalpost::class.java)) {

    private val JournalpostIdKey = "@behov.$behov.journalpostId"
    private val VersjonKey = "@behov.$behov.versjon"
    private fun identitetsnummerKey(part: String) = "@behov.$behov.$part.identitetsnummer"
    private fun saksnummerKey(part: String) = "@behov.$behov.$part.saksnummer"

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(behov)
                packet.interestedIn(VersjonKey)
                packet.require(JournalpostIdKey, JsonNode::asText)
                packet.require(identitetsnummerKey("fra"), JsonNode::asText)
                packet.require(saksnummerKey("fra"), JsonNode::asText)
                packet.require(identitetsnummerKey("til"), JsonNode::asText)
                packet.require(saksnummerKey("til"), JsonNode::asText)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        return (packet[VersjonKey].asText() == "1.0.0").also { støttet -> if (!støttet) {
            logger.warn("Støtter ikke versjon ${packet[VersjonKey].asText()}")
        }}
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet["@correlationId"].asText()

        val journalpost = Journalpost(
            journalpostId = packet[JournalpostIdKey].asText(),
            identitetsnummer = packet[identitetsnummerKey("fra")].asText(),
            saksnummer = packet[saksnummerKey("fra")].asText(),
            fagsaksystem = fagsystem
        )

        logger.info("Kopierer JournalpostId=[${journalpost.journalpostId}] for Fagsystem=[${journalpost.fagsaksystem.name}]")

        val erFerdigstilt = journalforingMediator.behandlaJournalpost(
            correlationId = correlationId,
            journalpost = journalpost
        )

        if (!erFerdigstilt) {
            return false
        }

        val nyJournalpostId = runBlocking { dokarkivproxyClient.knyttTilAnnenSak(
            correlationId = correlationId,
            journalpost = journalpost.copy(
                identitetsnummer = packet[identitetsnummerKey("til")].asText(),
                saksnummer = packet[saksnummerKey("til")].asText()
            )
        )}

        packet.leggTilLøsning(behov, mapOf(
            "journalpostId" to nyJournalpostId
        ))

        logger.info("Kopiert JournalpostId=[$nyJournalpostId]")

        return true
    }
}