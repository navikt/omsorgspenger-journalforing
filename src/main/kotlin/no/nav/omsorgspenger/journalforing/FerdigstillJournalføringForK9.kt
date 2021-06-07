package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.Fagsystem

internal class FerdigstillJournalføringForK9(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator
) : FerdigstillJournalforing(
    rapidsConnection = rapidsConnection,
    journalforingMediator = journalforingMediator,
    behov = "FerdigstillJournalføringForK9",
    fagsystem = Fagsystem.K9
)