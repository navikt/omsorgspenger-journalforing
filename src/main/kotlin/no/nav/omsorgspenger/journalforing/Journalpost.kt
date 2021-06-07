package no.nav.omsorgspenger.journalforing

import no.nav.omsorgspenger.Fagsystem

internal data class Journalpost(
    val journalpostId: String,
    val identitetsnummer: String,
    val saksnummer: String,
    val fagsaksystem: Fagsystem
)

