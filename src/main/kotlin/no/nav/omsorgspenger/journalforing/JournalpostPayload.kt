package no.nav.omsorgspenger.journalforing

internal data class JournalpostPayload internal constructor(
    val journalpostId: String,
    val bruker: Bruker,
    val sak: Sak,
    val tema: String = "OMS") {

    internal data class Bruker(
        val id: String,
        val idType: String = "FNR"
    )

    internal data class Sak(
        val sakstype: String = "FAGSAK",
        val fagsakId: String,
        val fagsaksystem: String = "OMSORGSPENGER"
    )
}