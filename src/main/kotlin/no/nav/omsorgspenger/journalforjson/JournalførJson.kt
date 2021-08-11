package no.nav.omsorgspenger.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import org.slf4j.LoggerFactory

internal class JournalførJson(
    rapidsConnection: RapidsConnection
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(JournalførJson::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(BEHOV)?.also { aktueltBehov ->
                    packet.require(aktueltBehov.json()) { it.requireObject() }
                    packet.require(aktueltBehov.tittel()) { it.requireText() }
                    packet.require(aktueltBehov.fagsystem()) { it.requireText() }
                    packet.require(aktueltBehov.saksnummer()) { it.requireText() }
                    packet.require(aktueltBehov.navn()) { it.requireText() }
                    packet.require(aktueltBehov.identitesnummer()) { it.requireText() }
                    packet.interestedIn(aktueltBehov.farge())
                }
            }
        }.register(this)
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()

        val fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].asText())
        val tittel = packet[aktueltBehov.tittel()].asText()
        val farge = packet[aktueltBehov.farge()].farge()
        val saksnummer = packet[aktueltBehov.saksnummer()].asText().somSaksnummer()
        val identitetsnummer = packet[aktueltBehov.identitesnummer()].asText().somIdentitetsnummer()
        val navn = packet[aktueltBehov.navn()].asText()
        val json = packet[aktueltBehov.json()] as ObjectNode

        logger.info("Journalfører Json for Fagsystem=[${fagsystem.name}] på Saksnummer=[$saksnummer] med Farge=[$farge] & Tittel=[$tittel]")

        val pdf = PdfGenerator.genererPdf(
            html = HtmlGenerator.genererHtml(
                tittel = packet[aktueltBehov.tittel()].asText(),
                farge = packet[aktueltBehov.farge()].farge(),
                json = json
            )
        )

        // https://confluence.adeo.no/display/BOA/opprettJournalpost

        packet.leggTilLøsning(aktueltBehov, mapOf(
            "journalpostId" to "123"
        ))

        return true
    }

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        asText().matches(FARGE_REGEX) -> asText()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${asText()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    private companion object {
        private const val BEHOV = "JournalførJson"
        private val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
        private const val DEFAULT_FARGE = "#C1B5D0"
        private fun String.json() = "@behov.$this.json"
        private fun String.tittel() = "@behov.$this.tittel"
        private fun String.farge() = "@behov.$this.farge"
        private fun String.fagsystem() = "@behov.$this.fagsystem"
        private fun String.navn() = "@behov.$this.person.navn"
        private fun String.identitesnummer() = "@behov.$this.person.identitesnummer"
        private fun String.saksnummer() = "@behov.$this.person.saksnummer"
    }
}