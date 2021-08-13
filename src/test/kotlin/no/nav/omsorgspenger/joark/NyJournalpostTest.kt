package no.nav.omsorgspenger.joark

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.ZonedDateTime

internal class NyJournalpostTest {

    @Test
    fun `genrerer payload til dokarkiv`() {
        JSONAssert.assertEquals(forventetPayload, nyJournalpost.dokarkivPayload(), true)
    }

    internal companion object {
        @Language("JSON")
        private val json = """
        {
          "søknad": {
            "id": "1",
            "foo": true
          },
          "liste": ["en", "to"]
        }
        """.trimIndent().let { jacksonObjectMapper().readTree(it) as ObjectNode }

        internal val nyJournalpost = NyJournalpost(
            behovssekvensId = "01FCTKFCGACDNTBH7V56VZ598X",
            tittel = "Test tittel",
            brevkode = "NAV brevkode 007",
            mottatt = ZonedDateTime.parse("2021-05-03T16:08:45.800Z"),
            fagsystem = Fagsystem.K9,
            saksnummer = "ABC123".somSaksnummer(),
            identitetsnummer = "11111111111".somIdentitetsnummer(),
            avsenderNavn = "Ola Nordmann",
            json = json,
            pdf = "LiksomPdf".toByteArray()
        )

        @Language("JSON")
        private val forventetPayload = """
        {
          "eksternReferanseId": "01FCTKFCGACDNTBH7V56VZ598X",
          "datoMottatt": "2021-05-03T16:08:45.800Z",
          "tittel": "Test tittel",
          "avsenderMottaker": {
            "navn": "Ola Nordmann"
          },
          "bruker": {
            "id": "11111111111",
            "idType": "FNR"
          },
          "sak": {
            "sakstype": "FAGSAK",
            "fagsakId": "ABC123",
            "fagsaksystem": "K9"
          },
          "dokumenter": [{
            "tittel": "Test tittel",
            "brevkode": "NAV brevkode 007",
            "dokumentVarianter": [{
              "filtype": "PDFA",
              "variantformat": "ARKIV",
              "fysiskDokument": "TGlrc29tUGRm"
            },{
              "filtype": "JSON",
              "variantformat": "ORIGINAL",
              "fysiskDokument": "eyJzw7hrbmFkIjp7ImlkIjoiMSIsImZvbyI6dHJ1ZX0sImxpc3RlIjpbImVuIiwidG8iXX0="
            }]
          }],
          "tema": "OMS",
          "journalposttype": "NOTAT",
          "kanal": "INGEN_DISTRIBUSJON",
          "journalfoerendeEnhet": "9999"
        }
        """.trimIndent()
    }
}