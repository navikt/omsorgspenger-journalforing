package no.nav.omsorgspenger.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveRespons(
    val id: String,
    val journalpostId: String,
)

