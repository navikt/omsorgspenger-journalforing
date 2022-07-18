package no.nav.omsorgspenger

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import no.nav.helse.dusseldorf.ktor.health.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.omsorgspenger.ferdigstilljournalforing.FerdigstillJournalføringRiver
import no.nav.omsorgspenger.journalforjson.JournalførJsonRiver
import no.nav.omsorgspenger.kopierjournalpost.KopierJournalpostRiver
import no.nav.omsorgspenger.oppgave.OpprettGosysJournalføringsoppgaverRiver

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(
        config = RapidApplication.RapidApplicationConfig.fromEnv(env = applicationContext.env)
    )
        .withKtorModule { omsorgspengerJournalføring(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    OpprettGosysJournalføringsoppgaverRiver(
        rapidsConnection = this,
        oppgaveClient = applicationContext.oppgaveClient
    )
    JournalførJsonRiver(
        rapidsConnection = this,
        dokarkivClient = applicationContext.dokarkivClient
    )
    FerdigstillJournalføringRiver(
        rapidsConnection = this,
        dokarkivClient = applicationContext.dokarkivClient,
        safGateway = applicationContext.safGateway
    )
    KopierJournalpostRiver(
        rapidsConnection = this,
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
            override suspend fun check(): Result {
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