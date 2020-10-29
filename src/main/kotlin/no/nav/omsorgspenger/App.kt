package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.routing.*
import java.net.URI
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.hentRequiredEnv
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing
import no.nav.omsorgspenger.journalforing.JournalforingMediator

fun main() {
    val applicationContext = ApplicationContext.Builder().build()
    RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(applicationContext.env))
        .withKtorModule { omsorgspengerJournalføring(applicationContext) }
        .build()
        .apply { registerApplicationContext(applicationContext) }
        .start()
}

internal fun RapidsConnection.registerApplicationContext(applicationContext: ApplicationContext) {
    FerdigstillJournalforing(
        rapidsConnection = this,
        journalforingMediator = applicationContext.journalforingMediator
    )
    register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            applicationContext.start()
        }
        override fun onShutdown(rapidsConnection: RapidsConnection) {
            applicationContext.stop()
        }
    })
}

internal fun Application.omsorgspengerJournalføring(applicationContext: ApplicationContext) {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        HealthRoute(healthService = applicationContext.healthService)
    }
}

internal class ApplicationContext(
    internal val env: Environment,
    internal val joarkClient: JoarkClient,
    internal val journalforingMediator: JournalforingMediator,
    internal val healthService: HealthService) {

    internal fun start() {}
    internal fun stop() {}

    internal class Builder(
        internal var env: Environment? = null,
        internal var httpClient: HttpClient? = null,
        internal val accessTokenClient: AccessTokenClient? = null,
        internal var joarkClient: JoarkClient? = null,
        internal var journalforingMediator: JournalforingMediator? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetHttpClient = httpClient?:HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
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

            return ApplicationContext(
                env = benyttetEnv,
                joarkClient = benyttetJoarkClient,
                journalforingMediator = journalforingMediator?: JournalforingMediator(
                    joarkClient = benyttetJoarkClient
                ),
                healthService = HealthService(healthChecks = setOf(
                    benyttetJoarkClient
                ))
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
        }
    }
}