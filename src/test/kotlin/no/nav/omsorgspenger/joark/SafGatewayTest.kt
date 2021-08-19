package no.nav.omsorgspenger.joark

import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.joark.SafGateway.Companion.førsteJournalpostIdSomHarOriginalJournalpostId
import no.nav.omsorgspenger.joark.SafGateway.Companion.hentOriginalJournalpostIderQuery
import no.nav.omsorgspenger.joark.SafGateway.Companion.mapOriginaleJournalpostIderResponse
import no.nav.omsorgspenger.Saksnummer.Companion.somSaksnummer
import no.nav.omsorgspenger.joark.SafGateway.Companion.safData
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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