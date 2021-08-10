package no.nav.omsorgspenger.ferdigstilljournalforing

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.Fagsystem

internal class FerdigstillJournalføringForK9(
    rapidsConnection: RapidsConnection,
    ferdigstillJournalføringMediator: FerdigstillJournalføringMediator
) : FerdigstillJournalføring(
    rapidsConnection = rapidsConnection,
    ferdigstillJournalføringMediator = ferdigstillJournalføringMediator,
    behov = "FerdigstillJournalføringForK9",
    fagsystem = Fagsystem.K9
)