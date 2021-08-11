package no.nav.omsorgspenger.joark

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.rapid.behov.Behovsformat.iso8601
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Saksnummer
import org.intellij.lang.annotations.Language
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

internal data class NyJournalpost(
    internal val behovssekvensId: String,
    internal val tittel: String,
    internal val mottatt: ZonedDateTime,
    internal val brevkode: String,
    internal val fagsystem: Fagsystem,
    internal val saksnummer: Saksnummer,
    internal val identitetsnummer: Identitetsnummer,
    internal val avsenderNavn: String,
    internal val pdf: ByteArray,
    internal val json: ObjectNode) {

    internal fun dokarkivPayload() : String {
        @Language("JSON")
        val json = """
            {
              "eksternReferanseId": "$behovssekvensId",
              "datoMottatt": "${mottatt.withZoneSameInstant(UTC).iso8601()}",
              "tittel": "$tittel",
              "avsenderMottaker": {
                "navn": "$avsenderNavn"
              },
              "bruker": {
                "id": "$identitetsnummer",
                "idType": "FNR"
              },
              "sak": {
                "sakstype": "FAGSAK",
                "fagsakId": "$saksnummer",
                "fagsaksystem": "${fagsystem.name}"
              },
              "dokumenter": [{
                "tittel": "$tittel",
                "brevkode": "$brevkode",
                "dokumentVarianter": [{
                  "filtype": "PDFA",
                  "variantformat": "ARKIV",
                  "fysiskDokument": "${pdf.base64()}"
                },{
                  "filtype": "JSON",
                  "variantformat": "ORIGINAL",
                  "fysiskDokument": "${json.base64()}"
                }]
              }],
              "tema": "OMS",
              "journalposttype": "N",
              "kanal": "INGEN_DISTRIBUSJON",
              "journalfoerendeEnhet": "9999"
            }
        """.trimIndent()
        return json
    }

    private companion object {
        private val UTC = ZoneId.of("UTC")
        private fun ByteArray.base64() = Base64.getUrlEncoder().encodeToString(this)
        private fun ObjectNode.base64() = this.toString().toByteArray().base64()
    }
}