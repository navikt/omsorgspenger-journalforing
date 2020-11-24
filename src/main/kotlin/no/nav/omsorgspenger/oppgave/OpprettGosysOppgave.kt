package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.behov.Behovsformat
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.harLøsningPåBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireArray
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.OppgaveClient
import no.nav.omsorgspenger.incBehandlingUtfort
import no.nav.omsorgspenger.incMottattBehov
import org.slf4j.LoggerFactory

internal class OpprettGosysOppgave(
        rapidsConnection: RapidsConnection,
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
                packet.require(AKTOERID, JsonNode::asText)
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        logger.info("Skal løse behov $BEHOV").also { incMottattBehov() }

        val journalpostIder = packet[JOURNALPOSTIDER]
                .map { it.asText() }
                .toSet()
        val journalpostType = packet[JOURNALPOSTTYPE].asText()
        val aktorId = packet[AKTOERID].asText()
        val correlationId = packet[Behovsformat.CorrelationId].asText()

        logger.info("Henter oppgaver för ${journalpostIder.size} journal id(er)")
        var losning = runBlocking {
            return@runBlocking oppgaveClient.hentOppgave(correlationId, aktorId, journalpostIder)
        }.filterKeys { journalpostIder.contains(it) }
        logger.info("Hentet ${losning.size} oppgaver")

        journalpostIder.filterNot {
            losning.keys.contains(it)
        }.forEach { jId ->
            val result = runBlocking {
                return@runBlocking oppgaveClient.opprettOppgave(correlationId, Oppgave(
                        journalpostType = journalpostType,
                        journalpostId = jId,
                        aktoerId = aktorId))
            }
            if(result.containsKey(jId)) {
                losning = losning.plus(result)
            }
        }

        require(losning.keys.containsAll(journalpostIder)) { "Klarade inte att opprette eller hente oppgave för alla journalpostID"}

        packet.leggTilLøsning(BEHOV, mapOf(
                "oppgaveIder" to losning)
        )
        return true
    }

    override fun onSent(id: String, packet: JsonMessage) {
        logger.info("Løst behov $BEHOV").also { incBehandlingUtfort() }
    }

    internal companion object {
        const val BEHOV = "OpprettGosysJournalføringsoppgaver"
        const val JOURNALPOSTIDER = "@behov.$BEHOV.journalpostIder"
        const val JOURNALPOSTTYPE = "@behov.$BEHOV.journalpostType"
        const val AKTOERID = "@løsninger.HentPersonopplysninger.personopplysninger.attributer.aktørId"
    }
}