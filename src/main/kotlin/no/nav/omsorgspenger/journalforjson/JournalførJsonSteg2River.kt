package no.nav.omsorgspenger.journalforjson

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import org.slf4j.LoggerFactory

internal class JournalførJsonSteg2River(
    rapidsConnection: RapidsConnection
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(JournalførJsonSteg2River::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(JournalførJsonMelding.BehovNavn)?.also { aktueltBehov ->
                    JournalførJsonMelding.validateBehov(packet, aktueltBehov)
                }
                packet.harLøsningPåBehov(HentNavnMelding.BehovNavn)
                HentNavnMelding.validateLøsning(packet)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val journalførJson = JournalførJsonMelding.hentBehov(packet, aktueltBehov)
        val navn = HentNavnMelding.hentLøsning(packet, journalførJson.identitetsnummer)

        logger.info("Journalfører Json for Fagsystem=[${journalførJson.fagsystem.name}] på Saksnummer=[${journalførJson.saksnummer}] med Farge=[${journalførJson.farge}] & Tittel=[${journalførJson.tittel}]")

        logger.info("Genrerer PDF")
        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = journalførJson.tittel,
                farge = journalførJson.farge,
                json = journalførJson.json
            )
        )

        // https://confluence.adeo.no/display/BOA/opprettJournalpost

        packet.leggTilLøsning(aktueltBehov, mapOf(
            "journalpostId" to "123"
        ))

        return true
    }
}