package no.nav.omsorgspenger.joark

import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Saksnummer
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.json.JSONObject

internal data class FerdigstillJournalpost(
    internal val journalpostId: JournalpostId,
    private val status: JoarkTyper.JournalpostStatus,
    private val avsendernavn: String? = null,
    private val tittel: String? = null,
    private val dokumenter: Set<Dokument> = emptySet(),
    private val bruker: Bruker? = null) {

    private val mangler = mutableListOf<Mangler>().also { alleMangler ->
        if (bruker == null) { alleMangler.add(Mangler.Bruker) }
        if (avsendernavn.isNullOrBlank() && bruker?.navn.isNullOrBlank()) { alleMangler.add(Mangler.Avsendernavn) }
    }.toList()

    internal val erFerdigstilt = status.erFerdigstilt || status.erJournalført
    internal val kanFerdigstilles = !erFerdigstilt && mangler.isEmpty()

    private fun mangler() : List<Mangler> {
        check(!erFerdigstilt) { "Journalpost $journalpostId er allerede ferdigstilt." }
        return mangler
    }

    internal fun manglerAvsendernavn() = mangler().contains(Mangler.Avsendernavn)

    internal fun oppdaterPayload() : String {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
        @Language("JSON")
        val json = """
        {
          "tema": "OMS",
          "bruker": {
            "idType": "FNR",
            "id": "${bruker!!.identitetsnummer}"
          },
          "sak": {
            "sakstype": "FAGSAK",
            "fagsaksystem": "${bruker.sak.first.name}",
            "fagsakId": "${bruker.sak.second}"
          }
        }
        """.trimIndent().let { JSONObject(it) }

        // Mangler tittel på journalposten
        if (tittel.isNullOrBlank()) { json.put("tittel", ManglerTittel) }
        // Mangler tittel på et eller fler dokumenter
        dokumenter.filter { it.tittel.isNullOrBlank() }.takeIf { it.isNotEmpty() }?.also { dokumenterUtenTittel ->
            val jsonDokumenter = JSONArray()
            dokumenterUtenTittel.forEach { dokumentUtenTittel ->
                jsonDokumenter.put(JSONObject().also {
                    it.put("dokumentInfoId", dokumentUtenTittel.dokumentId)
                    it.put("tittel", ManglerTittel)
                })
            }
            json.put("dokumenter", jsonDokumenter)
        }
        // Mangler navn på avsender
        if (avsendernavn.isNullOrBlank()) {
            json.put("avsenderMottaker", JSONObject().also { it.put("navn", bruker.navn!!) })
        }
        return json.toString()
    }

    internal fun ferdigstillPayload() : String {
        check(kanFerdigstilles) { "Journalposten $journalpostId kan ikke ferdigstilles." }
        return FerdigstillPayload
    }

    internal data class Bruker(
        internal val identitetsnummer: Identitetsnummer,
        internal val sak: Pair<Fagsystem, Saksnummer>,
        internal val navn: String?
    )

    internal data class Dokument(
        internal val dokumentId: String,
        internal val tittel: String?
    )

    internal enum class Mangler {
        Bruker,
        Avsendernavn
    }

    private companion object {
        private const val ManglerTittel = "Mangler tittel"
        @Language("JSON")
        private const val FerdigstillPayload = """{"journalfoerendeEnhet": "9999"}"""
    }
}