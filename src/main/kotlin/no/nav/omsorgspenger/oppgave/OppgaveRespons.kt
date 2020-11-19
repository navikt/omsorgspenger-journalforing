package no.nav.omsorgspenger.oppgave

data class OppgaveRespons(
    val id: String,
    val aktoerId: String,
    val journalpostId: String,
    val tema: String,
    val prioritet: String,
    val aktivDato: String
)

