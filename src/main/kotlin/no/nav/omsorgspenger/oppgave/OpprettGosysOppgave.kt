package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireArray
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.OppgaveClient
import no.nav.omsorgspenger.journalforing.incBehandlingUtfort
import no.nav.omsorgspenger.journalforing.incMottattBehov
import org.slf4j.LoggerFactory

internal class OpprettGosysOppgave(
        private val rapidsConnection: RapidsConnection,
        private val oppgaveClient: OppgaveClient) : BehovssekvensPacketListener(
        logger = LoggerFactory.getLogger(OpprettGosysOppgave::class.java)
) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)
                packet.harLøsningPåBehov("HentPersonopplysninger")
                packet.require(JOURNALPOSTIDER) { it.requireArray { entry -> entry is TextNode } }
                packet.require(JOURNALPOSTTYPE, JsonNode::asText)
                packet.interestedIn(AKTOERID, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $BEHOV med id $id").also { incMottattBehov() }

        val journalpostIder = packet[JOURNALPOSTIDER]
                .map { it.asText() }
                .toSet()
        val journalpostType = packet[JOURNALPOSTTYPE].asText()
        val aktorId = packet[AKTOERID].asText()

        val losning = runBlocking {
            return@runBlocking oppgaveClient.opprettOppgave("hantererbehov", Oppgave(
                    journalpostType = journalpostType,
                    journalpostId = journalpostIder,
                    aktoerId = aktorId
            )
            ).id
        }

        packet.leggTilLøsning(BEHOV, mapOf(
                "oppgaveIder" to setOf(losning))
        )

        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV med id $id").also { incBehandlingUtfort() }
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"
        const val JOURNALPOSTIDER = "@behov.$BEHOV.journalpostIder"
        const val JOURNALPOSTTYPE = "@behov.$BEHOV.journalpostType"
        const val AKTOERID = "@løsninger.HentPersonopplysninger.aktørId"
    }
}