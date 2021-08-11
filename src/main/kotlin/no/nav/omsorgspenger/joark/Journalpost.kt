package no.nav.omsorgspenger.joark

import no.nav.omsorgspenger.Fagsystem

internal data class Journalpost(
    val journalpostId: String,
    val identitetsnummer: String,
    val saksnummer: String,
    val navn: String? = null,
    val fagsaksystem: Fagsystem
)

