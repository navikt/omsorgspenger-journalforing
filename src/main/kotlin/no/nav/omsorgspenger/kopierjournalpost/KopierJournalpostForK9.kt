package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.DokarkivproxyClient
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.SafGateway
import no.nav.omsorgspenger.journalforing.JournalforingMediator

internal class KopierJournalpostForK9(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator,
    dokarkivproxyClient: DokarkivproxyClient,
    safGateway: SafGateway
) : KopierJournalpost(
    rapidsConnection = rapidsConnection,
    journalforingMediator = journalforingMediator,
    dokarkivproxyClient = dokarkivproxyClient,
    safGateway = safGateway,
    fagsystem = Fagsystem.K9,
    behov = "KopierJournalpostForK9"
)