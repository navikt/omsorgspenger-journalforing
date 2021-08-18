package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer

internal object KopierJournalpostMelding {

    internal fun validateBehov(packet: JsonMessage, aktueltBehov: String) {
        packet.requireKey(
            aktueltBehov.tilIdentitetsnummer(),
            aktueltBehov.tilSaksnummer(),
            aktueltBehov.journalpostId(),
            aktueltBehov.fagsystem()
        )
        packet.interestedIn(
            aktueltBehov.fraIdentitetsnummer(),
            aktueltBehov.fraSaksnummer()
        )
    }

    internal fun hentBehov(packet: JsonMessage, aktueltBehov: String) = KopierJournalpost(
        journalpostId = packet[aktueltBehov.journalpostId()].asText().somJournalpostId(),
        fraIdentitetsnummer = when (packet[aktueltBehov.fraIdentitetsnummer()].isMissingOrNull()) {
            true -> null
            false -> packet[aktueltBehov.fraIdentitetsnummer()].asText().somIdentitetsnummer()
        },
        fraSaksnummer = when (packet[aktueltBehov.fraSaksnummer()].isMissingOrNull()) {
            true -> null
            false -> packet[aktueltBehov.fraSaksnummer()].asText().somSaksnummer()
        },
        tilIdentitetsnummer = packet[aktueltBehov.tilIdentitetsnummer()].asText().somIdentitetsnummer(),
        tilSaksnummer = packet[aktueltBehov.tilSaksnummer()].asText().somSaksnummer(),
        fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].asText())
    )

    internal fun leggTilLøsning(packet: JsonMessage, aktueltBehov: String, journalpostId: JournalpostId) {
        packet.leggTilLøsning(
            behov = aktueltBehov,
            løsning = mapOf(
                "journalpostId" to "$journalpostId"
            )
        )
    }

    internal data class KopierJournalpost(
        internal val journalpostId: JournalpostId,
        internal val fraIdentitetsnummer: Identitetsnummer?,
        internal val fraSaksnummer: Saksnummer?,
        internal val tilIdentitetsnummer: Identitetsnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val fagsystem: Fagsystem) {
        internal val inneholderFraInformasjon = fraIdentitetsnummer != null && fraSaksnummer != null
    }

    private fun String.fraIdentitetsnummer() = "@behov.$this.fra.identitetsnummer"
    private fun String.fraSaksnummer() = "@behov.$this.fra.saksnummer"
    private fun String.tilIdentitetsnummer() = "@behov.$this.til.identitetsnummer"
    private fun String.tilSaksnummer() = "@behov.$this.til.saksnummer"
    private fun String.journalpostId() = "@behov.$this.journalpostId"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
}