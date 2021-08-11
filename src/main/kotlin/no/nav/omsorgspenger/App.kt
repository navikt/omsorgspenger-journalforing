package no.nav.omsorgspenger

import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.health.*
import java.net.URI
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringForK9
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringForOmsorgspenger
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringMediator
import no.nav.omsorgspenger.joark.DokarkivproxyClient
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.kopierjournalpost.KopierJournalpostForK9
import no.nav.omsorgspenger.oppgave.InitierGosysJournalføringsoppgaver
import no.nav.omsorgspenger.oppgave.OpprettGosysJournalføringsoppgaver

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule { omsorgspengerJournalføring(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    FerdigstillJournalføringForOmsorgspenger(
        rapidsConnection = this,
        ferdigstillJournalføringMediator = applicationContext.ferdigstillJournalføringMediator
    )
    FerdigstillJournalføringForK9(
        rapidsConnection = this,
        ferdigstillJournalføringMediator = applicationContext.ferdigstillJournalføringMediator
    )
    OpprettGosysJournalføringsoppgaver(
        rapidsConnection = this,
        oppgaveClient = applicationContext.oppgaveClient
    )
    InitierGosysJournalføringsoppgaver(
        rapidsConnection = this
    )

    KopierJournalpostForK9(
        rapidsConnection = this,
        ferdigstillJournalføringMediator = applicationContext.ferdigstillJournalføringMediator,
        dokarkivproxyClient = applicationContext.dokarkivproxyClient,
        safGateway = applicationContext.safGateway
    )

    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })
    register(RapidsStateListener(onStateChange = { state -> applicationContext.rapidsState = state }))
}

internal fun Application.omsorgspengerJournalføring(applicationContext: ApplicationContext) {
    install(ContentNegotiation) {
        jackson()
    }

    val healthService = HealthService(
        healthChecks = applicationContext.healthChecks.plus(object : HealthCheck {
            override suspend fun check() : Result {
                val currentState = applicationContext.rapidsState
                return when (currentState.isHealthy()) {
                    true -> Healthy("RapidsConnection", currentState.asMap)
                    false -> UnHealthy("RapidsConnection", currentState.asMap)
                }
            }
        })
    )

    HealthReporter(
        app = "omsorgspenger-journalforing",
        healthService = healthService
    )

    routing {
        HealthRoute(healthService = healthService)
    }
}

internal class ApplicationContext(
    internal val env: Environment,
    internal val dokarkivClient: DokarkivClient,
    internal val dokarkivproxyClient: DokarkivproxyClient,
    internal val safGateway: SafGateway,
    internal val ferdigstillJournalføringMediator: FerdigstillJournalføringMediator,
    internal val oppgaveClient: OppgaveClient,
    internal val healthChecks: Set<HealthCheck>) {
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal fun start() {}
    internal fun stop() {}

    internal class Builder(
        internal var env: Environment? = null,
        internal var httpClient: HttpClient? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var dokarkivClient: DokarkivClient? = null,
        internal var dokarkivproxyClient: DokarkivproxyClient? = null,
        internal var safGateway: SafGateway? = null,
        internal var ferdigstillJournalføringMediator: FerdigstillJournalføringMediator? = null,
        internal var oppgaveClient: OppgaveClient? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient()
                .config { expectSuccess = false }
            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))
            )
            val benyttetDokarkivClient = dokarkivClient?: DokarkivClient(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient
            )
            val benyttetOppgaveClient = oppgaveClient?: OppgaveClient(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient
            )

            val benyttetDokarkivproxyClient = dokarkivproxyClient ?: DokarkivproxyClient(
                accessTokenClient = benyttetAccessTokenClient,
                baseUrl = URI(benyttetEnv.hentRequiredEnv("DOKARKIVPROXY_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("DOKARKIVPROXY_SCOPES").csvTilSet()
            )

            val benyttetSafGateway = safGateway ?: SafGateway(
                accessTokenClient = benyttetAccessTokenClient,
                baseUrl = URI(benyttetEnv.hentRequiredEnv("SAF_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("SAF_SCOPES").csvTilSet()
            )

            return ApplicationContext(
                env = benyttetEnv,
                dokarkivClient = benyttetDokarkivClient,
                ferdigstillJournalføringMediator = ferdigstillJournalføringMediator?: FerdigstillJournalføringMediator(
                    dokarkivClient = benyttetDokarkivClient
                ),
                healthChecks = setOf(
                    benyttetDokarkivClient,
                    benyttetOppgaveClient,
                    benyttetDokarkivproxyClient,
                    benyttetSafGateway
                ),
                oppgaveClient = benyttetOppgaveClient,
                dokarkivproxyClient = benyttetDokarkivproxyClient,
                safGateway = benyttetSafGateway
            )
        }
    }
}