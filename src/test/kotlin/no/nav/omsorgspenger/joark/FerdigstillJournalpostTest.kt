package no.nav.omsorgspenger.joark

import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class FerdigstillJournalpostTest {

    @Test
    fun `håndterer avsendernavn`() {
        val ferdigstillJournalpost = FerdigstillJournalpost(
            journalpostId = "11111111".somJournalpostId(),
            status = "MOTATTT".somJournalpostStatus(),
            avsendernavn = null
        )

        val bruker = FerdigstillJournalpost.Bruker(
            identitetsnummer = "11111111111".somIdentitetsnummer(),
            sak = Fagsystem.K9 to "ABC123".somSaksnummer(),
            navn = null
        )

        assertFalse(ferdigstillJournalpost.kanFerdigstilles)

        assertFalse(ferdigstillJournalpost.copy(bruker = bruker).kanFerdigstilles)

        assertTrue(ferdigstillJournalpost.copy(bruker = bruker, avsendernavn = "Ola Nordmann").also {
            it.assertAvsendernavnTilDokarkiv(null)
        }.kanFerdigstilles)

        assertTrue(ferdigstillJournalpost.copy(bruker = bruker.copy(navn = "Kari Nordmann"), avsendernavn = null).also {
            it.assertAvsendernavnTilDokarkiv("Kari Nordmann")
        }.kanFerdigstilles)

        ferdigstillJournalpost.copy(bruker = bruker.copy(navn = "Kari Nordmann"), avsendernavn = "Ola Nordmann").also {
            it.assertAvsendernavnTilDokarkiv(null)
        }
    }

    @Test
    fun `håndterer manglende tittel på hovedokument`() {
        val ferdigstillJournalpost = FerdigstillJournalpost(
            journalpostId = "1111111".somJournalpostId(),
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = "Ola Nordmann",
            tittel = null,
            bruker = FerdigstillJournalpost.Bruker(
                identitetsnummer = "11111111111".somIdentitetsnummer(),
                sak = Fagsystem.K9 to "ABC123".somSaksnummer(),
                navn = null
            ),
            dokumenter = setOf(FerdigstillJournalpost.Dokument(
                dokumentId = "1",
                tittel = "En tittel"
            ))
        )

        @Language("JSON")
        val forventet = """
        {
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "11111111111"
            },
            "sak": {
                "fagsaksystem": "K9",
                "sakstype": "FAGSAK",
                "fagsakId": "ABC123"
            },
            "tittel": "Mangler tittel"
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, ferdigstillJournalpost.oppdaterPayload(), true)
    }

    @Test
    fun `håndterer manglende tittel på dokumenter`() {
        val ferdigstillJournalpost = FerdigstillJournalpost(
            journalpostId = "1111111".somJournalpostId(),
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = "Ola Nordmann",
            tittel = "Har tittel",
            bruker = FerdigstillJournalpost.Bruker(
                identitetsnummer = "11111111111".somIdentitetsnummer(),
                sak = Fagsystem.K9 to "ABC123".somSaksnummer(),
                navn = null
            ),
            dokumenter = setOf(
                FerdigstillJournalpost.Dokument(dokumentId = "1", tittel = null),
                FerdigstillJournalpost.Dokument(dokumentId = "2", tittel = "Tittel 2"),
                FerdigstillJournalpost.Dokument(dokumentId = "3", tittel = " ")
            )
        )

        @Language("JSON")
        val forventet = """
        {
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "11111111111"
            },
            "sak": {
                "fagsaksystem": "K9",
                "sakstype": "FAGSAK",
                "fagsakId": "ABC123"
            },
            "dokumenter": [{
                "dokumentInfoId": "1",
                "tittel": "Mangler tittel"
            },{
                "dokumentInfoId": "3",
                "tittel": "Mangler tittel"
            }]
        }
        """.trimIndent()


        println(ferdigstillJournalpost.oppdaterPayload())

        JSONAssert.assertEquals(forventet, ferdigstillJournalpost.oppdaterPayload(), true)
    }

    @Test
    fun `håndterer ingen mangler`() {
        val ferdigstillJournalpost = FerdigstillJournalpost(
            journalpostId = "1111111".somJournalpostId(),
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = "Ola Nordmann",
            tittel = "Hovedtittel",
            bruker = FerdigstillJournalpost.Bruker(
                identitetsnummer = "11111111111".somIdentitetsnummer(),
                sak = Fagsystem.K9 to "ABC123".somSaksnummer(),
                navn = null
            ),
            dokumenter = setOf(FerdigstillJournalpost.Dokument(
                dokumentId = "1",
                tittel = "En tittel"
            ))
        )

        @Language("JSON")
        val forventet = """
        {
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "11111111111"
            },
            "sak": {
                "fagsaksystem": "K9",
                "sakstype": "FAGSAK",
                "fagsakId": "ABC123"
            }
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, ferdigstillJournalpost.oppdaterPayload(), true)
    }

    @Test
    fun `håndterer brukers navn som avsendernavn`() {
        val ferdigstillJournalpost = FerdigstillJournalpost(
            journalpostId = "1111111".somJournalpostId(),
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = null,
            tittel = "Hovedtittel",
            bruker = FerdigstillJournalpost.Bruker(
                identitetsnummer = "11111111111".somIdentitetsnummer(),
                sak = Fagsystem.K9 to "ABC123".somSaksnummer(),
                navn = "Kari Nordmann"
            ),
            dokumenter = setOf(FerdigstillJournalpost.Dokument(
                dokumentId = "1",
                tittel = "En tittel"
            ))
        )

        @Language("JSON")
        val forventet = """
        {
            "tema": "OMS",
            "bruker": {
                "idType": "FNR",
                "id": "11111111111"
            },
            "sak": {
                "fagsaksystem": "K9",
                "sakstype": "FAGSAK",
                "fagsakId": "ABC123"
            },
            "avsenderMottaker": {
              "navn": "Kari Nordmann"
            }
        }
        """.trimIndent()

        JSONAssert.assertEquals(forventet, ferdigstillJournalpost.oppdaterPayload(), true)
    }

    private companion object {
        private fun FerdigstillJournalpost.assertAvsendernavnTilDokarkiv(forventet: String?) {
            val json = JSONObject(oppdaterPayload())
            if (forventet == null) {
                assertFalse(json.has("avsenderMottaker"))
            } else {
                val avsenderMottaker = json.getJSONObject("avsenderMottaker")
                assertEquals(forventet, avsenderMottaker.getString("navn"))
            }
        }
    }
}