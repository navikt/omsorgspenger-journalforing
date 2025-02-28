package no.nav.omsorgspenger.kopierjournalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import no.nav.k9.rapid.river.BehovssekvensPacketListener
import no.nav.k9.rapid.river.aktueltBehov
import no.nav.k9.rapid.river.skalLøseBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.JoarkTyper
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.joark.SafGateway.Companion.førsteJournalpostIdSomHarOriginalJournalpostId
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class KopierJournalpostRiver(
    rapidsConnection: RapidsConnection,
    private val dokarkivClient: DokarkivClient,
    private val safGateway: SafGateway,
) : BehovssekvensPacketListener(
    logger = LoggerFactory.getLogger(KopierJournalpostRiver::class.java)) {

    init {
        River(rapidsConnection).apply {
            validate { packet ->
                packet.skalLøseBehov(KopierJournalpostMelding.behovNavn)?.also { aktueltBehov ->
                    KopierJournalpostMelding.validateBehov(packet, aktueltBehov)
                }
            }
        }.register(this)
    }

    override fun doHandlePacket(id: String, packet: JsonMessage): Boolean {
        val kopierJournalpost = KopierJournalpostMelding.hentBehov(packet)
        return (kopierJournalpost.versjon == "1.0.0").also { if (!it) {
            logger.warn("Støtter ikke ${KopierJournalpostMelding.behovNavn} på versjon ${kopierJournalpost.versjon}")
        }}
    }

    override fun handlePacket(id: String, packet: JsonMessage): Boolean {
        val aktueltBehov = packet.aktueltBehov()
        val correlationId = packet.correlationId()
        val kopierJournalpost = KopierJournalpostMelding.hentBehov(packet, aktueltBehov)

        logger.info("Kopierer JournalpostId=[${kopierJournalpost.journalpostId}] for Fagsystem=[${kopierJournalpost.fagsystem.name}] fra Saksnummer=[${kopierJournalpost.fraSaksnummer}] til Saksnummer=[${kopierJournalpost.tilSaksnummer}]")

        val opprettet = packet["@behovOpprettet"].takeIf { !it.isMissingOrNull() }?.asText() ?: packet["@opprettet"].asText()
        // Håndterer om journalposten allerede er kopiert.
        val alleredeKopiertJournalpostId = runBlocking { safGateway.hentOriginaleJournalpostIder(
            fagsystem = kopierJournalpost.fagsystem,
            saksnummer = kopierJournalpost.tilSaksnummer,
            fraOgMed = ZonedDateTime.parse(opprettet).minusWeeks(1).toLocalDate(),
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
            val nyJournalpostId = runBlocking { dokarkivClient.knyttTilAnnenSak(
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

        check(typeOgStatus.kanKopieresEtterFerdigstilling()) {
            "Journalpost kan ikke kopieres. Type=[${typeOgStatus.first}], Status=[${typeOgStatus.second}]"
        }

        // Legger til behov for å ferdigstille
        logger.info("Journalpost må ferdigstilles før den kopieres. Legger til behov for ferdigstilling.")
        KopierJournalpostMelding.leggTilBehovForFerdigstilling(
            packet = packet,
            aktueltBehov = aktueltBehov,
            kopierJournalpost = kopierJournalpost
        )
        return true
    }

    private companion object {
        private fun Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus>.kanKopieresNå() =
            (first.erNotat && second.erFerdigstilt) || (first.erInngående && second.erJournalført)

        private fun Pair<JoarkTyper.JournalpostType, JoarkTyper.JournalpostStatus>.kanKopieresEtterFerdigstilling() =
            !kanKopieresNå() && (first.erNotat || first.erInngående)
    }
}