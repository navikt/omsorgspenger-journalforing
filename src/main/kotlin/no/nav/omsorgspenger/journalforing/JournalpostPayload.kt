package no.nav.omsorgspenger.journalforing

class JournalpostPayload internal constructor(
        val journalpostId: String,
        val bruker: Bruker,
        val sak: Sak,
        val tema: String = "OMS"
) {

    data class Bruker(
            val id: String,
            val idType: String = "FNR"
    )

    data class Sak(
            val sakstype: String = "FAGSAK",
            val fagsakId: String,
            val fagsaksystem: String = "OMSORGSPENGER"
    )
}