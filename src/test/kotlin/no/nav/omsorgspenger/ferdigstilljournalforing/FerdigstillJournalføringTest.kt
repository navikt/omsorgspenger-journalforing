package no.nav.omsorgspenger.ferdigstilljournalforing

import de.huxhorn.sulky.ulid.ULID
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.river.leggTilLøsning
import no.nav.omsorgspenger.*
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.FerdigstillJournalpost
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.behov
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.løsninger
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.sisteMeldingErKlarForArkivering
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.sisteMeldingSomJsonMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class FerdigstillJournalføringTest(
    private val applicationContextBuilder: ApplicationContext.Builder) {

    private val dokarkivClientMock = mockk<DokarkivClient>().also { mock ->
        coEvery { mock.oppdaterJournalpostForFerdigstilling(any(), any()) }.returns(Unit)
        coEvery { mock.ferdigstillJournalposten(any(), any()) }.returns(Unit)
    }

    private val safGatewayMock = mockk<SafGateway>()

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContextBuilder.also { builder ->
            builder.dokarkivClient = dokarkivClientMock
            builder.safGateway = safGatewayMock
        }.build())
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
        clearMocks(safGatewayMock)
    }

    @Test
    fun `ferdigstille journalposter som allerede er ferdigstilt`() {
        val journalpost1 = "12345678".somJournalpostId()
        val journalpost2 = "910111213".somJournalpostId()

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost1) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost1,
            status = "FERDIGSTILT".somJournalpostStatus()
        ))

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost2) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost2,
            status = "JOURNALFOERT".somJournalpostStatus()
        ))

        val (_, behovsskevens) = nyBehovsSekvens(
            journalpostIder = setOf(journalpost1, journalpost2)
        )

        rapid.sendTestMessage(behovsskevens)
        assertEquals(setOf(FerdigstillJournalføring), rapid.behov())
        rapid.sisteMeldingErKlarForArkivering()
    }

    @Test
    fun `mangler avsendernavn på en journalpost`() {
        val journalpost1 = "12345678".somJournalpostId()
        val journalpost2 = "910111213".somJournalpostId()
        val identitetsnummer = "22222222222".somIdentitetsnummer()

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost1) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost1,
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = null
        ))

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost2) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost2,
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = "Ola Nordmann"
        ))

        val (_, behovsskevens) = nyBehovsSekvens(
            identitetsnummer = identitetsnummer,
            journalpostIder = setOf(journalpost1, journalpost2)
        )

        rapid.sendTestMessage(behovsskevens)
        assertEquals(setOf(HentNavn, FerdigstillJournalføring), rapid.behov())
        assertEquals(emptySet<String>(), rapid.løsninger())

        rapid.mockHentNavn(
            identitetsnummer = identitetsnummer,
            navn = "Kjell Nordmann"
        )

        assertEquals(setOf(HentNavn, FerdigstillJournalføring), rapid.løsninger())
        rapid.sisteMeldingErKlarForArkivering()
    }

    @Test
    fun `har avsendernavn på journalpost som skal ferdigstilles`() {
        val journalpost1 = "12345678".somJournalpostId()
        val journalpost2 = "910111213".somJournalpostId()
        val behovNavn = "$FerdigstillJournalføring@testCase"

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost1) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost1,
            status = "JOURNALFORT".somJournalpostStatus(),
            avsendernavn = "Kari Nordmann"
        ))

        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpost2) }.returns(FerdigstillJournalpost(
            journalpostId = journalpost2,
            status = "MOTTATT".somJournalpostStatus(),
            avsendernavn = "Ola Nordmann"
        ))

        val (_, behovsskevens) = nyBehovsSekvens(
            behov = behovNavn,
            journalpostIder = setOf(journalpost1, journalpost2)
        )

        rapid.sendTestMessage(behovsskevens)
        assertEquals(setOf(behovNavn), rapid.behov())

        rapid.sisteMeldingErKlarForArkivering()
    }

    private companion object {
        private const val HentNavn = "HentPersonopplysninger@ferdigstillJournalføring"
        private const val FerdigstillJournalføring = "FerdigstillJournalføring"

        private fun TestRapid.mockHentNavn(identitetsnummer: Identitetsnummer, navn: String) {
            sendTestMessage(sisteMeldingSomJsonMessage().leggTilLøsning(
                behov = HentNavn, løsning = mapOf(
                    "personopplysninger" to mapOf(
                        "$identitetsnummer" to mapOf(
                            "navn" to mapOf(
                                "sammensatt" to navn
                            )
                        )
                    )
                )
            ).toJson())
        }

        private fun nyBehovsSekvens(
            behov: String = FerdigstillJournalføring,
            id: String = ULID().nextULID(),
            identitetsnummer: Identitetsnummer = "11111111111".somIdentitetsnummer(),
            journalpostIder: Set<JournalpostId>,
            correlationId: String = UUID.randomUUID().toString(),
            fagsystem: Fagsystem = Fagsystem.K9
        ) = Behovssekvens(
            id = id,
            correlationId = correlationId,
            behov = arrayOf(
                Behov(
                    navn = behov,
                    input = mapOf(
                        "versjon" to "1.0.0",
                        "identitetsnummer" to "$identitetsnummer",
                        "journalpostIder" to journalpostIder.map { "$it" },
                        "saksnummer" to "a1b2c3",
                        "fagsystem" to fagsystem.name
                    )
                )
            )
        ).keyValue
    }
}