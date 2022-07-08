package no.nav.omsorgspenger.kopierjournalpost

import de.huxhorn.sulky.ulid.ULID
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.Identitetsnummer
import no.nav.omsorgspenger.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.omsorgspenger.JournalpostId
import no.nav.omsorgspenger.JournalpostId.Companion.somJournalpostId
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.FerdigstillJournalpost
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostStatus.Companion.somJournalpostStatus
import no.nav.omsorgspenger.joark.JoarkTyper.JournalpostType.Companion.somJournalpostType
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.registerApplicationContext
import no.nav.omsorgspenger.testutils.ApplicationContextExtension
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.behov
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.løsningPå
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.løsninger
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.sisteMelding
import no.nav.omsorgspenger.testutils.rapid.TestRapidVerktøy.sisteMeldingErKlarForArkivering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ApplicationContextExtension::class)
internal class KopierJournalpostRiverTest(
    private val applicationContextBuilder: ApplicationContext.Builder) {

    private val safGatewayMock = mockk<SafGateway>()
    private val dokarkivClientMock = mockk<DokarkivClient>().also { mock ->
        coEvery { mock.oppdaterJournalpostForFerdigstilling(any(), any()) }.returns(Unit)
        coEvery { mock.ferdigstillJournalpost(any(), any()) }.returns(Unit)
    }

    private val rapid = TestRapid().apply {
        this.registerApplicationContext(applicationContextBuilder.also { builder ->
            builder.safGateway = safGatewayMock
            builder.dokarkivClient = dokarkivClientMock
        }.build())
    }

    @BeforeEach
    internal fun reset() {
        rapid.reset()
        clearMocks(safGatewayMock)
    }

    @Test
    fun `Kopiere allerede kopiert journalpost`() {
        val journalpostId = "123456789".somJournalpostId()
        val (_, behovsskevens) = nyBehovsSekvens(
            journalpostId = journalpostId
        )

        coEvery { safGatewayMock.hentOriginaleJournalpostIder(any(),any(),any(),any()) }.returns(mapOf(
            "1011121314".somJournalpostId() to setOf(journalpostId)
        ))

        rapid.sendTestMessage(behovsskevens)
        assertEquals("1011121314", rapid.løsningPå(KopierJournalpost).getString("journalpostId"))
    }

    @Test
    fun `Kopiere allerede ferdigstilt journalpost`() {
        val (_, behovsskevens) = nyBehovsSekvens()

        coEvery { safGatewayMock.hentOriginaleJournalpostIder(any(),any(),any(),any()) }.returns(emptyMap())
        coEvery { safGatewayMock.hentTypeOgStatus(any(), any()) }.returns("N".somJournalpostType() to "FERDIGSTILT".somJournalpostStatus())
        rapid.sendTestMessage(behovsskevens)

        assertEquals(setOf(KopierJournalpost), rapid.behov())
        rapid.sisteMeldingErKlarForArkivering()
        assertEquals("123412341234", rapid.løsningPå(KopierJournalpost).getString("journalpostId"))
    }

    @Test
    fun `Kopiere fra og til samme person`() {
        val identitetsnummer = "11111111119".somIdentitetsnummer()
        val (_, behovsskevens) = nyBehovsSekvens(
            fra = identitetsnummer,
            til = identitetsnummer
        )

        coEvery { safGatewayMock.hentOriginaleJournalpostIder(any(),any(),any(),any()) }.returns(emptyMap())
        coEvery { safGatewayMock.hentTypeOgStatus(any(), any()) }.returns("I".somJournalpostType() to "JOURNALFOERT".somJournalpostStatus())
        rapid.sendTestMessage(behovsskevens)

        assertEquals(setOf(KopierJournalpost), rapid.behov())
        rapid.sisteMeldingErKlarForArkivering()
        assertEquals("123412341234", rapid.løsningPå(KopierJournalpost).getString("journalpostId"))
    }

    @Test
    fun `Kopiere journalpost som må ferdigstilles først`() {
        val journalpostId = "147258369".somJournalpostId()
        val behovNavn = "$KopierJournalpost@unitTest"
        val (_, behovsskevens) = nyBehovsSekvens(behov = behovNavn, journalpostId = journalpostId)

        coEvery { safGatewayMock.hentOriginaleJournalpostIder(any(),any(),any(),any()) }.returns(emptyMap())
        coEvery { safGatewayMock.hentTypeOgStatus(journalpostId, any()) }.returns("I".somJournalpostType() to "MOTTATT".somJournalpostStatus())

        rapid.sendTestMessage(behovsskevens)

        assertEquals(setOf(FerdigstillJournalføringBehov, behovNavn), rapid.behov())
        assertEquals(emptySet<String>(), rapid.løsninger())

        // Sender melding for å bli løst av FerdigstillJournalføringRiver
        coEvery { safGatewayMock.hentFerdigstillJournalpost(any(), journalpostId) }.returns(FerdigstillJournalpost(journalpostId = journalpostId, status = "MOTTATT".somJournalpostStatus(), type = "I".somJournalpostType(), avsendernavn = "Ola Nordmann"))
        rapid.sendTestMessage(rapid.sisteMelding())
        assertEquals(setOf(FerdigstillJournalføringBehov), rapid.løsninger())

        // Sender melding for å bli løst på nytt nå som journalposten er ferdigstilt
        coEvery { safGatewayMock.hentTypeOgStatus(journalpostId, any()) }.returns("I".somJournalpostType() to "JOURNALFOERT".somJournalpostStatus())
        rapid.sendTestMessage(rapid.sisteMelding())
        assertEquals(setOf(FerdigstillJournalføringBehov, behovNavn), rapid.løsninger())
        rapid.sisteMeldingErKlarForArkivering()
        assertEquals("123412341234", rapid.løsningPå(behovNavn).getString("journalpostId"))
    }

    @Test
    fun `håndterer deprecated @opprettet key`() {
        val identitetsnummer = "11111111119".somIdentitetsnummer()
        var (_, behovsskevens) = nyBehovsSekvens(
            fra = identitetsnummer,
            til = identitetsnummer
        )

        behovsskevens = behovsskevens.replace("@behovOpprettet", "@opprettet")

        coEvery { safGatewayMock.hentOriginaleJournalpostIder(any(),any(),any(),any()) }.returns(emptyMap())
        coEvery { safGatewayMock.hentTypeOgStatus(any(), any()) }.returns("I".somJournalpostType() to "JOURNALFOERT".somJournalpostStatus())
        rapid.sendTestMessage(behovsskevens)

        assertEquals(setOf(KopierJournalpost), rapid.behov())
        rapid.sisteMeldingErKlarForArkivering()
        assertEquals("123412341234", rapid.løsningPå(KopierJournalpost).getString("journalpostId"))
    }

    private companion object {
        private const val FerdigstillJournalføringBehov = "FerdigstillJournalføring@kopierJournalpost"
        const val KopierJournalpost = "KopierJournalpost"

        private fun nyBehovsSekvens(
            behov: String = KopierJournalpost,
            journalpostId: JournalpostId = "33333333333".somJournalpostId(),
            fra: Identitetsnummer = "11111111111".somIdentitetsnummer(),
            til: Identitetsnummer = "22222222222".somIdentitetsnummer(),
            id: String = ULID().nextULID(),
            correlationId: String = UUID.randomUUID().toString()
        ) = Behovssekvens(
            id = id,
            correlationId = correlationId,
            behov = arrayOf(
                Behov(
                    navn = behov,
                    input = mapOf(
                        "versjon" to "1.0.0",
                        "journalpostId" to "$journalpostId",
                        "fagsystem" to "K9",
                        "fra" to mapOf(
                            "identitetsnummer" to "$fra",
                            "saksnummer" to "SAK${fra}"
                        ),
                        "til" to mapOf(
                            "identitetsnummer" to "$til",
                            "saksnummer" to "SAK${til}"
                        )
                    )
                )
            )
        ).keyValue
    }
}