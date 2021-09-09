package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.k9.rapid.river.requireArray
import no.nav.omsorgspenger.AktørId
import no.nav.omsorgspenger.AktørId.Companion.somAktørId
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.OppgaveId

internal object OpprettGosysJournalføringsoppgaverMelding {

    internal data class OpprettGosysJournalføringsoppgaver(
        internal val identitetsnummer: Identitetsnummer,
        internal val berørteIdentitetsnummer: Set<Identitetsnummer>,
        internal val journalpostIder: Set<JournalpostId>,
        internal val journalpostType: String,
        internal val enhetsnummer: String?,
        internal val aktørId: AktørId?) {
        internal val manglerPersonopplysninger = enhetsnummer == null || aktørId == null
    }

    internal fun validateBehov(packet: JsonMessage) {
        packet.require(JOURNALPOSTIDER) { it.requireArray { entry -> entry is TextNode } }
        packet.require(JOURNALPOSTTYPE, JsonNode::asText)
        packet.require(IDENTITETSNUMMER, JsonNode::asText)
        packet.interestedIn(BERØRTEIDENTITETSNUMMER)
        packet.interestedIn(ENHETSNUMMER, JsonNode::asText)
        packet.interestedIn(PERSONOPPLYSNINGER_LØSNING)
    }

    internal fun hentBehov(packet: JsonMessage) : OpprettGosysJournalføringsoppgaver {
        val identitetsnummer = packet[IDENTITETSNUMMER].asText().somIdentitetsnummer()
        return OpprettGosysJournalføringsoppgaver(
            identitetsnummer = identitetsnummer,
            berørteIdentitetsnummer = when (packet[BERØRTEIDENTITETSNUMMER].isMissingOrNull()) {
                true -> emptySet()
                false -> packet[BERØRTEIDENTITETSNUMMER].map { it.asText().somIdentitetsnummer() }.toSet()
            },
            journalpostIder = packet[JOURNALPOSTIDER].map { it.asText().somJournalpostId() }.toSet(),
            journalpostType = packet[JOURNALPOSTTYPE].asText(),
            enhetsnummer = when (packet[ENHETSNUMMER].isMissingOrNull()) {
                true -> null
                false -> packet[ENHETSNUMMER].asText()
            },
            aktørId = when (packet[PERSONOPPLYSNINGER_LØSNING].isMissingOrNull()) {
                true -> null
                false -> packet[PERSONOPPLYSNINGER_LØSNING]["$identitetsnummer"]["aktørId"].asText().somAktørId()
            }
        )
    }

    internal fun leggTilBehovForPersonopplysninger(packet: JsonMessage) {
        val behov = hentBehov(packet)
        packet.leggTilBehov(aktueltBehov = behovNavn, Behov(
            navn = hentPersonopplysningerNavn,
            input = mapOf(
                "identitetsnummer" to behov.berørteIdentitetsnummer.plus(behov.identitetsnummer).map { "$it" },
                "attributter" to setOf("aktørId", "enhetsnummer")
            )
        ))
    }

    internal fun leggTilLøsning(packet: JsonMessage, løsning: Map<JournalpostId, OppgaveId>) {
        packet.leggTilLøsning(
            behov = behovNavn,
            løsning = mapOf("oppgaveIder" to løsning.mapValues { "${it.value}" })
        )
    }

    internal const val behovNavn = "OpprettGosysJournalføringsoppgaver"
    private const val hentPersonopplysningerNavn = "HentPersonopplysninger@opprettGosysJournalføringsoppgaver"

    private const val JOURNALPOSTIDER = "@behov.$behovNavn.journalpostIder"
    private const val JOURNALPOSTTYPE = "@behov.$behovNavn.journalpostType"
    private const val IDENTITETSNUMMER = "@behov.$behovNavn.identitetsnummer"
    private const val BERØRTEIDENTITETSNUMMER = "@behov.$behovNavn.berørteIdentitetsnummer"
    private const val ENHETSNUMMER = "@løsninger.$hentPersonopplysningerNavn.fellesopplysninger.enhetsnummer"
    private const val PERSONOPPLYSNINGER_LØSNING = "@løsninger.$hentPersonopplysningerNavn.personopplysninger"
}