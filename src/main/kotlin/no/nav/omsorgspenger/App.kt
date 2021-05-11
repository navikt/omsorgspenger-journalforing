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
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.journalforing.FerdigstillJournalføringForK9
import no.nav.omsorgspenger.journalforing.FerdigstillJournalføringForOmsorgspenger
import no.nav.omsorgspenger.journalforing.JournalforingMediator
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
        journalforingMediator = applicationContext.journalforingMediator
    )
    FerdigstillJournalføringForK9(
        rapidsConnection = this,
        journalforingMediator = applicationContext.journalforingMediator
    )
    OpprettGosysJournalføringsoppgaver(
        rapidsConnection = this,
        oppgaveClient = applicationContext.oppgaveClient
    )
    InitierGosysJournalføringsoppgaver(
        rapidsConnection = this
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
    internal val joarkClient: JoarkClient,
    internal val journalforingMediator: JournalforingMediator,
    internal val oppgaveClient: OppgaveClient,
    internal val healthChecks: Set<HealthCheck>) {
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal fun start() {}
    internal fun stop() {}

    internal class Builder(
        internal var env: Environment? = null,
        internal var httpClient: HttpClient? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var joarkClient: JoarkClient? = null,
        internal var journalforingMediator: JournalforingMediator? = null,
        internal var oppgaveClient: OppgaveClient? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient()
                .config { expectSuccess = false }
            val benyttetAccessTokenClient = accessTokenClient?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_APP_TOKEN_ENDPOINT"))
            )
            val benyttetJoarkClient = joarkClient?: JoarkClient(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient
            )
            val benyttetOppgaveClient = oppgaveClient?: OppgaveClient(
                env = benyttetEnv,
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient
            )

            return ApplicationContext(
                env = benyttetEnv,
                joarkClient = benyttetJoarkClient,
                journalforingMediator = journalforingMediator?: JournalforingMediator(
                    joarkClient = benyttetJoarkClient
                ),
                healthChecks = setOf(
                    benyttetJoarkClient,
                    benyttetOppgaveClient
                ),
                oppgaveClient = benyttetOppgaveClient
            )
        }
    }
}