package no.nav.omsorgspenger.ferdigstilljournalforing

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.k9.rapid.river.*
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.FerdigstillJournalpost
import no.nav.omsorgspenger.joark.SafGateway
import org.slf4j.LoggerFactory

internal class FerdigstillJournalføringRiver(
    rapidsConnection: RapidsConnection,
    private val safGateway: SafGateway,
    private val dokarkivClient: DokarkivClient
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(FerdigstillJournalføringRiver::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(FerdigstillJournalføringMelding.behovNavn)?.also { aktueltBehov ->
                    FerdigstillJournalføringMelding.validateBehov(packet, aktueltBehov)
                }
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val ferdigstillJournalføring = FerdigstillJournalføringMelding.hentBehov(packet)
        return (ferdigstillJournalføring.versjon == "1.0.0").also { if (!it) {
            logger.warn("Støtter ikke ${FerdigstillJournalføringMelding.behovNavn} på versjon ${ferdigstillJournalføring.versjon}")
        }}
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val correlationId = packet.correlationId()
        val behov = FerdigstillJournalføringMelding.hentBehov(packet, aktueltBehov)

        logger.info("Ferdigstiller JournalpostIder=${behov.journalpostIder} for Fagsystem=[${behov.fagsystem.name}] & Saksnummer=[${behov.saksnummer}]")

        val bruker = FerdigstillJournalpost.Bruker(
            identitetsnummer = behov.identitetsnummer,
            sak = behov.fagsystem to behov.saksnummer,
            navn = behov.navn?.also { logger.info("Har hentet navn på bruker.") }
        )

        val ferdigstillJournalposter = behov.journalpostIder.map { journalpostId -> runBlocking { safGateway.hentFerdigstillJournalpost(
            correlationId = correlationId,
            journalpostId = journalpostId
        )}}.filterNot { ferdigstillJournalpost ->
            ferdigstillJournalpost.erFerdigstilt.also { if (it) {
                logger.info("JournalpostId=[${ferdigstillJournalpost.journalpostId}] er allerede ferdigstilt.")
            }
        }}.map { it.copy(bruker = bruker) }

        val manglerAvsendernavn = ferdigstillJournalposter.filter { it.manglerAvsendernavn() }
        // Legger til behov for å hente navn på brukeren om det vi mangler avsendernavn
        if (manglerAvsendernavn.isNotEmpty()) {
            logger.info("Mangler avsendernavn på JournalpostIder=${manglerAvsendernavn.map { it.journalpostId }}. Legger til behov for å hente navn på bruker.")
            FerdigstillJournalføringMelding.leggTilBehovForNavn(
                packet = packet,
                aktueltBehov = aktueltBehov,
                identitetsnummer = behov.identitetsnummer
            )
            return true
        }

        // Alle journalposter klare til oppdatering & ferdigstilling
        check(ferdigstillJournalposter.all { it.kanFerdigstilles })

        runBlocking { ferdigstillJournalposter.forEach { ferdigstillJournalpost ->
            dokarkivClient.oppdaterJournalpostForFerdigstilling(correlationId, ferdigstillJournalpost)
            dokarkivClient.ferdigstillJournalpost(correlationId, ferdigstillJournalpost)
        }}

        logger.info("Alle journalposter ferdigstilt.")
        packet.leggTilLøsning(aktueltBehov)
        return true
    }
}