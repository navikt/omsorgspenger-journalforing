package no.nav.omsorgspenger.oppgave

import java.time.LocalDateTime
import java.time.ZoneOffset
import no.nav.omsorgspenger.extensions.DateUtils
import no.nav.omsorgspenger.extensions.StringExt.trimJson
import org.intellij.lang.annotations.Language

/*
REQUIRED per https://oppgave.nais.preprod.local/#/Oppgave/opprettOppgave:
Koder:  https://kodeverk-web.nais.preprod.local/kodeverksoversikt
*/

data class Oppgave(
    val journalpostId: Set<String>,
    val journalpostType: String,
    val aktoerId: String,
    val tema: String? = "OMS"
)

internal fun Oppgave.oppdatertOppgaveBody(): String {
    @Language("JSON")
    val json = """
        {
          "tema": "$tema",
          "journalpostId": "$journalpostId",
          "journalpostType": "$journalpostType",
          "behandlingsTema": "ab0149",
          "behandlingsType": "ae0227",
          "prioritet": "NORM",
          "journalpostkilde": "AS36",
          "temagruppe": "FMLI",
          "aktivDato": "${LocalDateTime.now(ZoneOffset.UTC)}",
          "frist": "${DateUtils.nWeekdaysFromToday(3)}",
          "oppgavetype": "JFR",
          "aktoerId": "$aktoerId"
        }
    """.trimIndent()
    return json.trimJson()
}

/*
        behandlendeEnhet = behandlendeEnhet,
        sokerAktoerId = sokerAktoerId,
        behandlesAv = INFOTRYGD_FAGSYSTEM,
*/