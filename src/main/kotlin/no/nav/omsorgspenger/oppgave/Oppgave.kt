package no.nav.omsorgspenger.oppgave

import java.time.LocalDateTime
import java.time.ZoneOffset
import no.nav.omsorgspenger.extensions.DateUtils
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import org.intellij.lang.annotations.Language

// Koder:  https://kodeverk-web.nais.preprod.local/kodeverksoversikt
data class Oppgave(
        var journalpostId: String,
        val journalpostType: String,
        val aktørId: String,
        val tema: String = "OMS",
        val behandlingsTema: String =
                OppgaveAttributter.hentAttributer(journalføringstype = journalpostType).behandlingstema,
        val behandlingsType: String =
                OppgaveAttributter.hentAttributer(journalføringstype = journalpostType).behandlingstype
)

internal fun Oppgave.oppdatertOppgaveBody(): String {
    // Oppgave schema: https://oppgave.nais.preprod.local/#/Oppgave/opprettOppgave
    @Language("JSON")
    val json = """
        {
          "tema": "$tema",
          "journalpostId": "$journalpostId",
          "behandlingstema": "$behandlingsTema",
          "behandlingstype": "$behandlingsType",
          "prioritet": "NORM",
          "journalpostkilde": "AS36",
          "temagruppe": "FMLI",
          "aktivDato": "${DateUtils.nWeekdaysFromToday(0)}",
          "fristFerdigstillelse": "${DateUtils.nWeekdaysFromToday(3)}",
          "oppgavetype": "JFR",
          "aktoerId": "$aktørId",
          "behandlesAvApplikasjon": "IT00"
        }
    """.trimIndent()
    return json.trimJson()
}