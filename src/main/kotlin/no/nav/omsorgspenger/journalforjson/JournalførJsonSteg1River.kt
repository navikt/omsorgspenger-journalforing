package no.nav.omsorgspenger.journalforjson

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import org.slf4j.LoggerFactory

internal class JournalførJsonSteg1River(
    rapidsConnection: RapidsConnection
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(JournalførJsonSteg1River::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(JournalførJsonMelding.BehovNavn)?.also { aktueltBehov ->
                    JournalførJsonMelding.validateBehov(packet, aktueltBehov)
                }
                packet.utenLøsningPåBehov(HentNavnMelding.BehovNavn)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val identitetsnummer = JournalførJsonMelding.hentBehov(packet, aktueltBehov).identitetsnummer
        logger.info("Legger til behov for å hente navn på personen.")
        packet.leggTilBehov(aktueltBehov, HentNavnMelding.behov(identitetsnummer))
        return true
    }
}