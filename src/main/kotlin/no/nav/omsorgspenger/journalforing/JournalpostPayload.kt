package no.nav.omsorgspenger.journalforing

class JournalpostPayload internal constructor(
        val journalpostId: String,
        private val bruker: Bruker,
        private val sak: Sak,
        private val tema: String = "OMS",
        val journalfoerendeEnhet: String = "9999"
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