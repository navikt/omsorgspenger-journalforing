package no.nav.omsorgspenger.joark

import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.joark.SafGateway.Companion.førsteJournalpostIdSomHarOriginalJournalpostId
import no.nav.omsorgspenger.joark.SafGateway.Companion.hentOriginalJournalpostIderQuery
import no.nav.omsorgspenger.joark.SafGateway.Companion.mapOriginaleJournalpostIderResponse
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.omsorgspenger.joark.SafGateway.Companion.hentFerdigstillJournalpostQuery
import no.nav.omsorgspenger.joark.SafGateway.Companion.hentTypeOgStatusQuery
import no.nav.omsorgspenger.joark.SafGateway.Companion.mapFerdigstillJournalpost
import no.nav.omsorgspenger.joark.SafGateway.Companion.mapTypeOgStatus
import no.nav.omsorgspenger.joark.SafGateway.Companion.safData
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SafGatewayTest {

    @Test
    fun `request for å hente originale journalpostIder`() {
        val forventet = """{"query":"query {dokumentoversiktFagsak(tema:OMS,fagsak:{fagsaksystem:\"K9\",fagsakId:\"SAK123\"},foerste:50,fraDato:\"2021-06-01\"){journalposter{journalpostId,dokumenter{originalJournalpostId}}}}"}"""

        val faktisk = hentOriginalJournalpostIderQuery(
            fagsystem = Fagsystem.K9,
            saksnummer = "SAK123".somSaksnummer(),
            fraOgMed = LocalDate.parse("2021-06-01")
        )

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `request for hente type og status`() {
        val journalpostId = "123123123".somJournalpostId()
        val forventet = """{"query":"query {journalpost(journalpostId:\"123123123\"){journalposttype,journalstatus}}"}"""

        val faktisk = hentTypeOgStatusQuery(journalpostId)
        assertEquals(forventet, faktisk)
    }

    @Test
    fun `map response på hente type og status`() {
        @Language("JSON")
        val response = """
        {
          "data": {
            "journalpost": {
              "journalposttype": "I",
              "journalstatus": "JOURNALFOERT"
            }
          }
        }
        """.trimIndent()

        val forventet = "I".somJournalpostType() to "JOURNALFOERT".somJournalpostStatus()
        val faktisk = response.safData().mapTypeOgStatus()
        assertEquals(forventet, faktisk)
        assertTrue(forventet.first.erInngående)
        assertTrue(forventet.second.erJournalført)
    }

    @Test
    fun `request for hente ferdigstill journalpost`() {
        val journalpostId = "123123123".somJournalpostId()
        val forventet = """{"query":"query {journalpost(journalpostId:\"123123123\"){journalstatus,tittel,avsenderMottaker{navn},dokumenter{dokumentInfoId,tittel}}}"}"""

        val faktisk = hentFerdigstillJournalpostQuery(journalpostId)
        assertEquals(forventet, faktisk)
    }

    @Test
    fun `map response på hente ferdigstill journalpost`() {
        val journalpostId = "123123123".somJournalpostId()
        val dokument = FerdigstillJournalpost.Dokument(
            dokumentId = "55555555",
            tittel = null
        )

        var forventet = FerdigstillJournalpost(
            journalpostId = journalpostId,
            status = "FERDIGSTILT".somJournalpostStatus(),
            avsendernavn = null,
            tittel = null,
            dokumenter = setOf(dokument)
        )

        assertEquals(forventet, hentFerdigstillJournalpostResponse(
            avsendernavn = null,
            tittel = null,
            dokumentTittel = null
        ).safData().mapFerdigstillJournalpost(journalpostId))

        forventet = forventet.copy(
            tittel = "En tittel satt",
            avsendernavn = "Ola Nordmann",
            dokumenter = setOf(dokument.copy(
                tittel = "En annen tittel satt"
            ))
        )

        assertEquals(forventet, hentFerdigstillJournalpostResponse(
            avsendernavn = "Ola Nordmann",
            tittel = "En tittel satt",
            dokumentTittel = "En annen tittel satt"
        ).safData().mapFerdigstillJournalpost(journalpostId))
    }

    private fun hentFerdigstillJournalpostResponse(
        avsendernavn: String?,
        tittel: String?,
        dokumentTittel: String?) : String {
        @Language("JSON")
        val response = """
        {
          "data": {
            "journalpost": {
              "journalstatus": "FERDIGSTILT",
              "tittel": ${tittel?.let { "\"$it\"" }},
              "avsenderMottaker": {
                "navn": ${avsendernavn?.let { "\"$it\"" }}
              },
              "dokumenter": [
                {
                  "dokumentInfoId": "55555555",
                  "tittel": ${dokumentTittel?.let { "\"$it\"" }}
                }
              ]
            }
          }
        }
        """.trimIndent()
        return response
    }

    @Test
    fun `map response på hente originale journalpostIder`() {
        @Language("JSON")
        val response = """
        {
          "data": {
            "dokumentoversiktFagsak": {
              "journalposter": [
                {
                  "journalpostId": "77777777777",
                  "dokumenter": [
                    { "originalJournalpostId": "77777777771" },
                    { "originalJournalpostId": "77777777772" }
                  ]
                },
                {
                  "journalpostId": "88888888888",
                  "dokumenter": [
                    { "originalJournalpostId": "88888888881" },
                    { "originalJournalpostId": null },
                    { "originalJournalpostId": "" }
                  ]
                },
                {
                  "journalpostId": "99999999999",
                  "dokumenter": []
                }
              ]
            }
          }
        }
        """.trimIndent()

        val forventet = mapOf(
            "77777777777".somJournalpostId() to setOf("77777777771".somJournalpostId(), "77777777772".somJournalpostId()),
            "88888888888".somJournalpostId() to setOf("88888888881".somJournalpostId()),
            "99999999999".somJournalpostId() to emptySet()
        )

        val faktisk = response.safData().mapOriginaleJournalpostIderResponse()

        assertEquals(forventet, faktisk)
    }

    @Test
    fun `finne første journalpostId gitt en original journalpostId`() {
        val originaleJournalpostIder = mapOf(
            "77777777777".somJournalpostId() to setOf("77777777771".somJournalpostId(), "77777777772".somJournalpostId()),
            "88888888888".somJournalpostId() to setOf("88888888881".somJournalpostId()),
            "99999999999".somJournalpostId() to setOf("88888888881".somJournalpostId())
        )

        assertEquals("88888888888".somJournalpostId(), originaleJournalpostIder.førsteJournalpostIdSomHarOriginalJournalpostId("88888888881".somJournalpostId()))
        assertNull(originaleJournalpostIder.førsteJournalpostIdSomHarOriginalJournalpostId("404404404".somJournalpostId()))
    }
}