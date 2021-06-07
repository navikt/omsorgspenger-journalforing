package no.nav.omsorgspenger.kopierjournalpost

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.DokarkivproxyClient
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.journalforing.JournalforingMediator

internal class KopierJournalpostForK9(
    rapidsConnection: RapidsConnection,
    journalforingMediator: JournalforingMediator,
    dokarkivproxyClient: DokarkivproxyClient
) : KopierJournalpost(
    rapidsConnection = rapidsConnection,
    journalforingMediator = journalforingMediator,
    dokarkivproxyClient = dokarkivproxyClient,
    fagsystem = Fagsystem.K9,
    behov = "KopierJournalpostForK9"
)