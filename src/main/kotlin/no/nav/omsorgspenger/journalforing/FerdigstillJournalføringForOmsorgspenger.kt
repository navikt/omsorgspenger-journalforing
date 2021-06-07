package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.Fagsystem

internal class FerdigstillJournalføringForOmsorgspenger(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator
) : FerdigstillJournalforing(
    rapidsConnection = rapidsConnection,
    journalforingMediator = journalforingMediator,
    behov = "FerdigstillJournalføringForOmsorgspenger",
    fagsystem = Fagsystem.OMSORGSPENGER
)