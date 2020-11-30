package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.k9.rapid.river.utenLøsningPåBehov
import no.nav.omsorgspenger.incBehandlingUtfort
import no.nav.omsorgspenger.incMottattBehov
import org.slf4j.LoggerFactory

internal class InitierGosysJournalføringsoppgaver(
        rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(InitierGosysJournalføringsoppgaver::class.java)
) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.utenLøsningPåBehov("HentPersonopplysninger")
                packet.require(IDENTITETSNUMMER, JsonNode::asText)
                packet.interestedIn(BERØRTEIDENTITETSNUMMER)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Behøver aktørid før $BEHOV")
                .also { incMottattBehov("InitierGosysJournalføringsoppgaver") }

        val identitetsnummer = packet[IDENTITETSNUMMER].asText()
        val berørteIdentitetsnummer = packet[BERØRTEIDENTITETSNUMMER]
                .map { it.asText() }
                .toSet()

        packet.leggTilBehov(
                aktueltBehov = "OpprettGosysJournalføringsoppgaver",
                behov = arrayOf(
                        Behov(
                                navn = "HentPersonopplysninger",
                                input = mapOf(
                                        "identitetsnummer" to berørteIdentitetsnummer.plus(identitetsnummer),
                                        "attributter" to setOf("aktørId", "enhetsnummer")
                                )
                        )
                )
        )

        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Sendt behov av personopplysninger åt $BEHOV")
                .also { incBehandlingUtfort("InitierGosysJournalføringsoppgaver") }
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"
        const val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
        const val BERØRTEIDENTITETSNUMMER = "@behov.$BEHOV.berørteIdentitetsnummer"
    }
}
