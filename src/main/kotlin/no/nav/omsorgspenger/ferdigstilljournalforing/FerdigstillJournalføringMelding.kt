package no.nav.omsorgspenger.ferdigstilljournalforing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.aktueltBehov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.requireArray
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer

internal object FerdigstillJournalføringMelding {

    internal data class FerdigstillJournalføring(
        internal val versjon: String,
        internal val journalpostIder: Set<JournalpostId>,
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: Saksnummer,
        internal val fagsystem: Fagsystem,
        internal val navn: String?
    )

    internal fun validateBehov(
        packet: JsonMessage,
        aktueltBehov: String) {
        packet.require(aktueltBehov.journalpostIder()) { it.requireArray { entry -> entry is TextNode } }
        packet.require(aktueltBehov.identitetsnummer(), JsonNode::asText)
        packet.require(aktueltBehov.saksnummer(), JsonNode::asText)
        packet.require(aktueltBehov.fagsystem(), JsonNode::asText)
        packet.interestedIn(personopplysninger)
    }

    internal fun hentBehov(
        packet: JsonMessage,
        aktueltBehov: String = packet.aktueltBehov()) : FerdigstillJournalføring {
        val identitetsnummer = packet[aktueltBehov.identitetsnummer()].asText().somIdentitetsnummer()
        return FerdigstillJournalføring(
            versjon = packet[aktueltBehov.versjon()].asText(),
            journalpostIder = packet[aktueltBehov.journalpostIder()].map { it.asText().somJournalpostId() }.toSet(),
            identitetsnummer = identitetsnummer,
            saksnummer = packet[aktueltBehov.saksnummer()].asText().somSaksnummer(),
            fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].asText()),
            navn = when (packet[personopplysninger].isMissingOrNull()) {
                true -> null
                false -> packet[personopplysninger].get("$identitetsnummer").get("navn").get("sammensatt").asText()
            })
    }

    internal fun leggTilBehovForNavn(packet: JsonMessage, aktueltBehov: String, identitetsnummer: Identitetsnummer) {
        packet.leggTilBehov(aktueltBehov,
            Behov(navn = hentPersonopplysningerBehov, input = mapOf(
                "attributter" to listOf("navn"),
                "identitetsnummer" to listOf("$identitetsnummer"),
                "måFinneAllePersoner" to true
            ))
        )
    }

    internal const val behovNavn = "FerdigstillJournalføring"
    private const val hentPersonopplysningerBehov = "HentPersonopplysninger@ferdigstillJournalføring"

    private fun String.journalpostIder() = "@behov.$this.journalpostIder"
    private fun String.identitetsnummer() = "@behov.$this.identitetsnummer"
    private fun String.saksnummer() = "@behov.$this.saksnummer"
    private fun String.versjon() = "@behov.$this.versjon"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
    private const val personopplysninger = "@løsninger.$hentPersonopplysningerBehov.personopplysninger"
}
