package no.nav.omsorgspenger.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireObject
import no.nav.k9.rapid.river.requireText
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer

import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal object JournalførJsonMelding {

    internal const val BehovNavn = "JournalførJson"

    internal fun validateBehov(packet: JsonMessage, aktueltBehov: String) {
        packet.require(aktueltBehov.json()) { it.requireObject() }
        packet.require(aktueltBehov.tittel()) { it.requireText() }
        packet.require(aktueltBehov.fagsystem()) { it.requireText() }
        packet.require(aktueltBehov.saksnummer()) { it.requireText() }
        packet.require(aktueltBehov.identitetsnummer()) { it.requireText() }
        packet.require(aktueltBehov.brevkode()) { it.requireText() }
        packet.require(aktueltBehov.avsenderNavn()) { it.requireText() }
        packet.require(aktueltBehov.mottatt()) { ZonedDateTime.parse(it.hentString()) }
        packet.interestedIn(aktueltBehov.farge())
    }

    internal fun hentBehov(packet: JsonMessage, aktueltBehov: String) = JournalførJson(
        json = packet[aktueltBehov.json()] as ObjectNode,
        tittel = packet[aktueltBehov.tittel()].hentString(),
        farge = packet[aktueltBehov.farge()].farge(),
        fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].hentString()),
        identitetsnummer = packet[aktueltBehov.identitetsnummer()].hentString().somIdentitetsnummer(),
        saksnummer = packet[aktueltBehov.saksnummer()].hentString().somSaksnummer(),
        brevkode = packet[aktueltBehov.brevkode()].hentString(),
        mottatt = packet[aktueltBehov.mottatt()].let { ZonedDateTime.parse(it.hentString()) },
        avsenderNavn = packet[aktueltBehov.avsenderNavn()].hentString()
    )

    internal fun leggTilLøsning(packet: JsonMessage, aktueltBehov: String, journalpostId: JournalpostId) {
        packet.leggTilLøsning(
            behov = aktueltBehov,
            løsning = mapOf(
                "journalpostId" to "$journalpostId"
            )
        )
    }

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        hentString().matches(FARGE_REGEX) -> hentString()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${hentString()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    internal data class JournalførJson(
        internal val json: ObjectNode,
        internal val brevkode: String,
        internal val tittel: String,
        internal val mottatt: ZonedDateTime,
        internal val farge: String,
        internal val fagsystem: Fagsystem,
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: Saksnummer,
        internal val avsenderNavn: String
    )

    private val logger = LoggerFactory.getLogger(JournalførJsonMelding::class.java)
    private val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
    private const val DEFAULT_FARGE = "#C1B5D0"
    private fun String.json() = "@behov.$this.json"
    private fun String.brevkode() = "@behov.$this.brevkode"
    private fun String.mottatt() = "@behov.$this.mottatt"
    private fun String.tittel() = "@behov.$this.tittel"
    private fun String.farge() = "@behov.$this.farge"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
    private fun String.identitetsnummer() = "@behov.$this.identitetsnummer"
    private fun String.saksnummer() = "@behov.$this.saksnummer"
    private fun String.avsenderNavn() = "@behov.$this.avsender.navn"
    private fun JsonNode.hentString() = asText().replace("\"","")
}