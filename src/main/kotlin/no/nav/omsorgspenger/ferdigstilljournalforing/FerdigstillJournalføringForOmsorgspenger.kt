package no.nav.omsorgspenger.ferdigstilljournalforing

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.Fagsystem

internal class FerdigstillJournalføringForOmsorgspenger(
    rapidsConnection: RapidsConnection,
    ferdigstillJournalføringMediator: FerdigstillJournalføringMediator
) : AbstractFerdigstillJournalføring(
    rapidsConnection = rapidsConnection,
    ferdigstillJournalføringMediator = ferdigstillJournalføringMediator,
    behov = "FerdigstillJournalføringForOmsorgspenger",
    fagsystem = Fagsystem.OMSORGSPENGER
)