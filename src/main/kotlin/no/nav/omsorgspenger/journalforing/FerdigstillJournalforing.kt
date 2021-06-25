package no.nav.omsorgspenger.journalforing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
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
                packet.interestedIn(PersonopplysningerKey)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $behov")

        val journalpostIder = packet[JOURNALPOSTIDER]
            .map { it.asText() }
            .toSet()
        val identitetsnummer = packet[IDENTITETSNUMMER].asText().somIdentitetsnummer()
        val saksnummer = packet[SAKSNUMMER].asText()
        val navn = packet.navnOrNull(identitetsnummer)

        logger.info("Saksnummer: $saksnummer, JournalpostIder: $journalpostIder, MedNavn=${navn != null}")

        journalpostIder.forEach {
            journalforingMediator.behandlaJournalpost(
                correlationId = packet["@correlationId"].asText(),
                journalpost = Journalpost(
                    journalpostId = it,
                    identitetsnummer = "$identitetsnummer",
                    saksnummer = saksnummer,
                    fagsaksystem = fagsystem,
                    navn = navn
                )
            ).let { success -> if (!success) {
                return when (navn) {
                    null -> packet.leggTilBehov(
                        aktueltBehov = behov,
                        behov = arrayOf(identitetsnummer.hentNavnBehov())
                    ).let {
                        logger.info("Legger til behov for å hente navn")
                        true
                    }
                    else -> false
                }
            }}
        }
        packet.leggTilLøsning(behov)
        logger.info("Løst behov $behov")
        return true
    }

    private companion object {
        private const val HentPersonopplysningerBehov = "HentPersonopplysninger@journalføring"
        private const val PersonopplysningerKey = "@løsninger.$HentPersonopplysningerBehov.personopplysninger"
        private fun Identitetsnummer.hentNavnBehov() = Behov(
            navn = HentPersonopplysningerBehov,
            input = mapOf(
                "attributter" to listOf("navn"),
                "identitetsnummer" to listOf("$this"),
                "måFinneAllePersoner" to true
            )
        )
        private fun JsonMessage.navnOrNull(identitetsnummer: Identitetsnummer) : String? {
            val personoplysninger = get(PersonopplysningerKey)
            return when {
                personoplysninger.isMissingOrNull() -> null
                else -> personoplysninger
                    .get("$identitetsnummer")
                    .get("navn")
                    .get("sammensatt")
                    .asText()
            }
        }
    }
}