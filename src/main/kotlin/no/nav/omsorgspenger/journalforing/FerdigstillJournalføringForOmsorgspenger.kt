package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.RapidsConnection

internal class FerdigstillJournalføringForOmsorgspenger(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator
) : FerdigstillJournalforing(rapidsConnection, journalforingMediator, "FerdigstillJournalføringForOmsorgspenger", "OMSORGSPENGER")