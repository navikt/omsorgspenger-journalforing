package no.nav.omsorgspenger.journalforjson

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.NyJournalpost
import org.slf4j.LoggerFactory

internal class JournalførJsonRiver(
    rapidsConnection: RapidsConnection,
    private val dokarkivClient: DokarkivClient
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(JournalførJsonRiver::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(JournalførJsonMelding.BehovNavn)?.also { aktueltBehov ->
                    JournalførJsonMelding.validateBehov(packet, aktueltBehov)
                }
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val journalførJson = JournalførJsonMelding.hentBehov(packet, aktueltBehov)

        logger.info("Journalfører Json for Fagsystem=[${journalførJson.fagsystem.name}] på Saksnummer=[${journalførJson.saksnummer}] med Brevkode=[${journalførJson.brevkode}] & Farge=[${journalførJson.farge}]")

        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = journalførJson.tittel,
                farge = journalførJson.farge,
                json = journalførJson.json
            )
        )

        val journalpostId = runBlocking { dokarkivClient.opprettJournalpost(
            correlationId = packet.correlationId(),
            nyJournalpost = NyJournalpost(
                behovssekvensId = id,
                tittel = journalførJson.tittel,
                brevkode = journalførJson.brevkode,
                fagsystem = journalførJson.fagsystem,
                saksnummer = journalførJson.saksnummer,
                identitetsnummer = journalførJson.identitetsnummer,
                avsenderNavn = journalførJson.avsenderNavn,
                pdf = pdf,
                json = journalførJson.json
            )
        )}

        logger.info("Opprettet JournalpostId=[$journalpostId]")

        JournalførJsonMelding.leggTilLøsning(
            packet = packet,
            aktueltBehov = aktueltBehov,
            journalpostId = journalpostId
        )

        return true
    }
}