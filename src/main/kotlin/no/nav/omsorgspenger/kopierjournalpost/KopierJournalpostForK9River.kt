package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.joark.DokarkivproxyClient
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringMediator

internal class KopierJournalpostForK9River(
    rapidsConnection: RapidsConnection,
    ferdigstillJournalføringMediator: FerdigstillJournalføringMediator,
    dokarkivproxyClient: DokarkivproxyClient,
    safGateway: SafGateway
) : AbstractKopierJournalpostRiver(
    rapidsConnection = rapidsConnection,
    ferdigstillJournalføringMediator = ferdigstillJournalføringMediator,
    dokarkivproxyClient = dokarkivproxyClient,
    safGateway = safGateway,
    fagsystem = Fagsystem.K9,
    behov = "KopierJournalpostForK9"
)