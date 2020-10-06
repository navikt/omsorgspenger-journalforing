package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing.Companion.BEHOV

class JournalpostPayload internal constructor(
        val journalpostId: String,
        private val bruker: Bruker,
        private val sak: Sak,
        private val tema: String = "OMS",
        val journalfoerendeEnhet: String = "9999"
) {

    constructor(packet: JsonMessage) :
            this(
                    journalpostId = packet["@behov.$BEHOV.journalpostid"].asText(),
                    bruker = Bruker(
                            id = packet["@behov.$BEHOV.identitetsnummer"].asText()
                    ),
                    sak = Sak(
                            fagsakId = packet["@behov.$BEHOV.saksnummer"].asText()
                    )
            )

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