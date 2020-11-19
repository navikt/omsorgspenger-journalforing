package no.nav.omsorgspenger.oppgave

data class Oppgave(
    val journalpostId: Set<String>,
    val journalpostType: String,
    val aktoerId: String
)

