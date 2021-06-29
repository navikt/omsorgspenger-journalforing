package no.nav.omsorgspenger.kopierjournalpost

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.DokarkivproxyClient
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.SafGateway
import no.nav.omsorgspenger.SafGateway.Companion.førsteJournalpostIdSomHarOriginalJournalpostId
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.journalforing.JournalforingMediator
import no.nav.omsorgspenger.journalforing.Journalpost
import no.nav.omsorgspenger.journalforing.JournalpostManglerNavn
import no.nav.omsorgspenger.journalforing.JournalpostManglerNavn.behandlaJournalpostHåndterManglerNavn
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal abstract class KopierJournalpost(
    rapidsConnection: RapidsConnection,
    private val journalforingMediator: JournalforingMediator,
    private val dokarkivproxyClient: DokarkivproxyClient,
    private val safGateway: SafGateway,
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
                packet.interestedIn(JournalpostManglerNavn.PersonopplysningerKey)
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        return (packet[VersjonKey].asText() == "1.0.0").also { støttet -> if (!støttet) {
            logger.warn("Støtter ikke versjon ${packet[VersjonKey].asText()}")
        }}
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val correlationId = packet.correlationId()
        val journalpostId = packet[JournalpostIdKey].asText().somJournalpostId()
        val fraIdentitetsnummer = packet[identitetsnummerKey("fra")].asText().somIdentitetsnummer()
        val tilIdentitetsnummer = packet[identitetsnummerKey("til")].asText().somIdentitetsnummer()
        val fraSaksnummer = packet[saksnummerKey("fra")].asText().somSaksnummer()
        val tilSaksnummer = packet[saksnummerKey("til")].asText().somSaksnummer()

        val journalpost = Journalpost(
            journalpostId = "$journalpostId",
            identitetsnummer = "$fraIdentitetsnummer",
            saksnummer = "$fraSaksnummer",
            fagsaksystem = fagsystem
        )

        logger.info("Kopierer JournalpostId=[${journalpostId}] for Fagsystem=[${fagsystem.name}]")

        val alleredeKopiertJournalpostId = runBlocking { safGateway.hentOriginaleJournalpostIder(
            fagsystem = fagsystem,
            saksnummer = tilSaksnummer,
            fraOgMed = ZonedDateTime.parse(packet["@opprettet"].asText()).minusWeeks(1).toLocalDate(),
            correlationId = correlationId
        )}.førsteJournalpostIdSomHarOriginalJournalpostId(journalpostId)

        if (alleredeKopiertJournalpostId != null) {
            logger.info("Journalpost allerede kopiert.")
            return packet.løsMed(alleredeKopiertJournalpostId)
        }

        return journalforingMediator.behandlaJournalpostHåndterManglerNavn(
            packet = packet,
            aktueltBehov = behov,
            identitetsnummer = fraIdentitetsnummer,
            saksnummer = fraSaksnummer,
            fagsystem = fagsystem,
            journalpostIder = setOf(journalpostId),
            onOk = {
                val nyJournalpostId = runBlocking { dokarkivproxyClient.knyttTilAnnenSak(
                    correlationId = correlationId,
                    journalpost = journalpost.copy(
                        identitetsnummer = "$tilIdentitetsnummer",
                        saksnummer = "$tilSaksnummer"
                    )
                )}

                packet.løsMed(nyJournalpostId)
            }
        )
    }

    private fun JsonMessage.løsMed(journalpostId: JournalpostId) : Boolean {
        leggTilLøsning(behov, mapOf(
            "journalpostId" to "$journalpostId"
        ))
        logger.info("Kopiert JournalpostId=[$journalpostId]")
        return true
    }
}