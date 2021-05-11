package no.nav.omsorgspenger.journalforing

import no.nav.helse.rapids_rivers.RapidsConnection

internal class FerdigstillJournalføringForK9(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator
) : FerdigstillJournalforing(rapidsConnection, journalforingMediator, "FerdigstillJournalføringForK9", "K9")