package no.nav.omsorgspenger.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.river.requireObject
import no.nav.k9.rapid.river.requireText
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer

import org.slf4j.LoggerFactory

internal object JournalførJsonMelding {

    internal const val BehovNavn = "JournalførJson"

    internal fun validateBehov(packet: JsonMessage, aktueltBehov: String) {
        packet.require(aktueltBehov.json()) { it.requireObject() }
        packet.require(aktueltBehov.tittel()) { it.requireText() }
        packet.require(aktueltBehov.fagsystem()) { it.requireText() }
        packet.require(aktueltBehov.saksnummer()) { it.requireText() }
        packet.require(aktueltBehov.identitesnummer()) { it.requireText() }
        packet.interestedIn(aktueltBehov.farge())
    }

    internal fun hentBehov(packet: JsonMessage, aktueltBehov: String) = JournalførJson(
        json = packet[aktueltBehov.json()] as ObjectNode,
        tittel = packet[aktueltBehov.tittel()].asText(),
        farge = packet[aktueltBehov.farge()].farge(),
        fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].asText()),
        identitetsnummer = packet[aktueltBehov.identitesnummer()].asText().somIdentitetsnummer(),
        saksnummer = packet[aktueltBehov.saksnummer()].asText().somSaksnummer()
    )

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        asText().matches(FARGE_REGEX) -> asText()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${asText()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    internal data class JournalførJson(
        internal val json: ObjectNode,
        internal val tittel: String,
        internal val farge: String,
        internal val fagsystem: Fagsystem,
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: Saksnummer
    )

    private val logger = LoggerFactory.getLogger(JournalførJsonMelding::class.java)
    private val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
    private const val DEFAULT_FARGE = "#C1B5D0"
    private fun String.json() = "@behov.$this.json"
    private fun String.tittel() = "@behov.$this.tittel"
    private fun String.farge() = "@behov.$this.farge"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
    private fun String.identitesnummer() = "@behov.$this.identitesnummer"
    private fun String.saksnummer() = "@behov.$this.saksnummer"
}