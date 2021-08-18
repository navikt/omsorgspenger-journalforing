package no.nav.omsorgspenger.kopierjournalpost

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.aktueltBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringMediator
import no.nav.omsorgspenger.joark.DokarkivproxyClient
import no.nav.omsorgspenger.joark.JoarkTyper
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.joark.SafGateway.Companion.førsteJournalpostIdSomHarOriginalJournalpostId
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class KopierJournalpostRiver(
    rapidsConnection: RapidsConnection,
    private val ferdigstillJournalføringMediator: FerdigstillJournalføringMediator,
    private val dokarkivproxyClient: DokarkivproxyClient,
    private val safGateway: SafGateway,
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierJournalpostRiver::class.java)) {

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val correlationId = packet.correlationId()
        val kopierJournalpost = KopierJournalpostMelding.hentBehov(packet, aktueltBehov)

        logger.info("Kopierer JournalpostId=[${kopierJournalpost.journalpostId}] for Fagsystem=[${kopierJournalpost.fagsystem.name}]")

        // Håndterer om journalposten allerede er kopiert.
        val alleredeKopiertJournalpostId = runBlocking { safGateway.hentOriginaleJournalpostIder(
            fagsystem = kopierJournalpost.fagsystem,
            saksnummer = kopierJournalpost.tilSaksnummer,
            fraOgMed = ZonedDateTime.parse(packet["@opprettet"].asText()).minusWeeks(1).toLocalDate(),
            correlationId = correlationId
        )}.førsteJournalpostIdSomHarOriginalJournalpostId(kopierJournalpost.journalpostId)

        if (alleredeKopiertJournalpostId != null) {
            logger.info("Journalpost allerede kopiert. JournalpostId=[$alleredeKopiertJournalpostId]")
            KopierJournalpostMelding.leggTilLøsning(
                packet = packet,
                aktueltBehov = aktueltBehov,
                journalpostId = alleredeKopiertJournalpostId
            )
            return true
        }

        // Henter type og status på journalposten for å avgjøre hva vi gjør videre.
        val typeOgStatus = runBlocking { safGateway.hentTypeOgStatus(
            journalpostId = kopierJournalpost.journalpostId,
            correlationId = packet.correlationId()
        )}

        // Om journalposten allerede er ferdigstilt kopierer vi den med en gang.
        if (typeOgStatus.kanKopieresNå()) {
            val nyJournalpostId = runBlocking { dokarkivproxyClient.knyttTilAnnenSak(
                journalpostId = kopierJournalpost.journalpostId,
                saksnummer = kopierJournalpost.tilSaksnummer,
                fagsystem = kopierJournalpost.fagsystem,
                identitetsnummer = kopierJournalpost.tilIdentitetsnummer,
                correlationId = correlationId
            )}

            logger.info("Journalpost kopiert. JournalpostId=[$nyJournalpostId]")
            KopierJournalpostMelding.leggTilLøsning(
                packet = packet,
                aktueltBehov = aktueltBehov,
                journalpostId = nyJournalpostId
            )
            return true
        }

        // Legger til behov for å ferdigstille
        if (typeOgStatus.måFerdigstillesFørKopiering() && kopierJournalpost.inneholderFraInformasjon) {
            logger.info("Journalpost må ferdigstilles før den kopieres. Legger til behov for ferdigstilling.")
            // TODO: legg til
            return true
        }

        throw IllegalStateException("Kan ikke kopiere Journalpost. JournalpostId=[${kopierJournalpost.journalpostId}], Type=[${typeOgStatus.first}], Status=[${typeOgStatus.second}], InneholderFraInformasjon=[${kopierJournalpost.inneholderFraInformasjon}]")
    }

    private companion object {
        private fun Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus>.kanKopieresNå() =
            (first.erNotat && second.erFerdigstilt) || (first.erInngående && second.erJournalført)

        private fun Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus>.måFerdigstillesFørKopiering() =
            first.erInngående && second.erMottatt
    }
}