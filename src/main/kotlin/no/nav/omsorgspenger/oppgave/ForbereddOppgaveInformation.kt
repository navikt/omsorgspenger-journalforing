package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.incBehandlingUtfort
import org.slf4j.LoggerFactory

internal class ForbereddOppgaveInformation(
        rapidsConnection: RapidsConnection) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(ForbereddOppgaveInformation::class.java)
) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.rejectKey(HENTPERSONOPPLYSNINGER_LOESNING)
                packet.require(IDENTITETSNUMMER, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Behøver aktørid før $BEHOV med id $id")

        val identitetsnummer = packet[IDENTITETSNUMMER].asText()

        packet.leggTilBehov(
                aktueltBehov = "OpprettGosysJournalføringsoppgaver",
                behov = arrayOf(
                        Behov(
                                navn = "HentPersonopplysninger",
                                input = mapOf(
                                        "identitetsnummer" to identitetsnummer,
                                        "attributter" to setOf("aktørId")
                                )
                        )
                )

        )

        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Sendt behov av personopplysninger før $BEHOV med id $id").also { incBehandlingUtfort() }
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"
        const val IDENTITETSNUMMER = "@behov.$BEHOV.identitetsnummer"
        const val HENTPERSONOPPLYSNINGER_LOESNING = "@løsninger.HentPersonopplysninger"
    }
}