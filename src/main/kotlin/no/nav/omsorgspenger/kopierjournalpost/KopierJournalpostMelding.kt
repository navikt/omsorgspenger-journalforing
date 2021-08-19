package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.aktueltBehov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringMelding

internal object KopierJournalpostMelding {

    internal data class KopierJournalpost(
        internal val versjon: String,
        internal val journalpostId: JournalpostId,
        internal val fraIdentitetsnummer: Identitetsnummer,
        internal val fraSaksnummer: Saksnummer,
        internal val tilIdentitetsnummer: Identitetsnummer,
        internal val tilSaksnummer: Saksnummer,
        internal val fagsystem: Fagsystem
    )

    internal fun validateBehov(
        packet: JsonMessage,
        aktueltBehov: String) {
        packet.requireKey(
            aktueltBehov.versjon(),
            aktueltBehov.tilIdentitetsnummer(),
            aktueltBehov.tilSaksnummer(),
            aktueltBehov.journalpostId(),
            aktueltBehov.fagsystem(),
            aktueltBehov.fraIdentitetsnummer(),
            aktueltBehov.fraSaksnummer()
        )
    }

    internal fun hentBehov(
        packet: JsonMessage,
        aktueltBehov: String = packet.aktueltBehov()) = KopierJournalpost(
        versjon = packet[aktueltBehov.versjon()].asText(),
        journalpostId = packet[aktueltBehov.journalpostId()].asText().somJournalpostId(),
        fraIdentitetsnummer = packet[aktueltBehov.fraIdentitetsnummer()].asText().somIdentitetsnummer(),
        fraSaksnummer = packet[aktueltBehov.fraSaksnummer()].asText().somSaksnummer(),
        tilIdentitetsnummer = packet[aktueltBehov.tilIdentitetsnummer()].asText().somIdentitetsnummer(),
        tilSaksnummer = packet[aktueltBehov.tilSaksnummer()].asText().somSaksnummer(),
        fagsystem = Fagsystem.valueOf(packet[aktueltBehov.fagsystem()].asText())
    )

    internal fun leggTilLøsning(
        packet: JsonMessage,
        aktueltBehov: String,
        journalpostId: JournalpostId) {
        packet.leggTilLøsning(
            behov = aktueltBehov,
            løsning = mapOf(
                "journalpostId" to "$journalpostId"
            )
        )
    }

    internal fun leggTilBehovForFerdigstilling(packet: JsonMessage, aktueltBehov: String, kopierJournalpost: KopierJournalpost) {
        packet.leggTilBehov(aktueltBehov,
            Behov(navn = ferdigstillJornalføringBehov, input = mapOf(
                "versjon" to "1.0.0",
                "identitetsnummer" to "${kopierJournalpost.fraIdentitetsnummer}",
                "saksnummer" to "${kopierJournalpost.fraSaksnummer}",
                "fagsystem" to kopierJournalpost.fagsystem.name,
                "journalpostIder" to listOf("${kopierJournalpost.journalpostId}")
            ))
        )
    }

    internal const val behovNavn = "KopierJournalpost"
    private const val ferdigstillJornalføringBehov = "${FerdigstillJournalføringMelding.behovNavn}@kopierJournalpost"
    private fun String.fraIdentitetsnummer() = "@behov.$this.fra.identitetsnummer"
    private fun String.fraSaksnummer() = "@behov.$this.fra.saksnummer"
    private fun String.tilIdentitetsnummer() = "@behov.$this.til.identitetsnummer"
    private fun String.tilSaksnummer() = "@behov.$this.til.saksnummer"
    private fun String.journalpostId() = "@behov.$this.journalpostId"
    private fun String.fagsystem() = "@behov.$this.fagsystem"
    private fun String.versjon() = "@behov.$this.versjon"

}