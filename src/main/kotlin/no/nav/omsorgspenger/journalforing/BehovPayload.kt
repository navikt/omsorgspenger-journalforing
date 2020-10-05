package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing.Companion.BEHOV
import java.util.UUID

class BehovPayload private constructor(
        val hendelseId: UUID,
        val journalpostId: String,
        private val tema: String,
        val journalfoerendeEnhet: String,
        private val bruker: Bruker,
        private val sak: Sak
) {

    constructor(packet: JsonMessage) :
            this(
                    hendelseId = UUID.fromString(packet["@id"].asText()),
                    journalpostId = packet["@behov.$BEHOV.journalpostid"].asText(),
                    tema = "OMS",
                    journalfoerendeEnhet = "9999",
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
            val fagsaksystem: String = "K9"
    )
}