package no.nav.omsorgspenger.ferdigstilljournalforing

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.leggTilBehov
import no.nav.omsorgspenger.CorrelationId.Companion.correlationId
import no.nav.omsorgspenger.Fagsystem
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.Saksnummer
import no.nav.omsorgspenger.joark.Journalpost
import org.slf4j.LoggerFactory

internal object JournalpostManglerNavn {

    internal fun FerdigstillJournalføringMediator.behandlaJournalpostHåndterManglerNavn(
        packet: JsonMessage,
        aktueltBehov: String,
        identitetsnummer: Identitetsnummer,
        saksnummer: Saksnummer,
        fagsystem: Fagsystem,
        journalpostIder: Set<JournalpostId>,
        onOk: () -> Boolean
    ) : Boolean {
        val navn = packet.navnOrNull(identitetsnummer)
        logger.info("MedNavn=${navn != null}")

        val resultat = journalpostIder.map { journalpostId ->
            behandlaJournalpost(
                correlationId = "${packet.correlationId()}",
                journalpost = Journalpost(
                    journalpostId = "$journalpostId",
                    identitetsnummer = "$identitetsnummer",
                    saksnummer = "$saksnummer",
                    fagsaksystem = fagsystem,
                    navn = navn
                )
            )}

        if (resultat.all { it }) {
            return onOk()
        }

        return when (navn) {
            null -> packet.leggTilBehov(
                aktueltBehov = aktueltBehov,
                behov = arrayOf(identitetsnummer.hentNavnBehov())
            ).let {
                logger.info("Legger til behov for å hente navn")
                true
            }
            else -> logger.warn("Journalføring feiler også når navn sendes med").let {
                false
            }
        }
    }

    private val logger = LoggerFactory.getLogger(JournalpostManglerNavn::class.java)

    private const val HentPersonopplysningerBehov = "HentPersonopplysninger@journalføring"
    internal const val PersonopplysningerKey = "@løsninger.$HentPersonopplysningerBehov.personopplysninger"
    private fun Identitetsnummer.hentNavnBehov() = Behov(
        navn = HentPersonopplysningerBehov,
        input = mapOf(
            "attributter" to listOf("navn"),
            "identitetsnummer" to listOf("$this"),
            "måFinneAllePersoner" to true
        )
    )
    private fun JsonMessage.navnOrNull(identitetsnummer: Identitetsnummer) : String? {
        val personoplysninger = get(PersonopplysningerKey)
        return when {
            personoplysninger.isMissingOrNull() -> null
            else -> personoplysninger
                .get("$identitetsnummer")
                .get("navn")
                .get("sammensatt")
                .asText()
        }
    }
}