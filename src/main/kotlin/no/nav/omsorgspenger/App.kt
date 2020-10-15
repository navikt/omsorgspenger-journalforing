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
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.config.readServiceUserCredentials
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing
import no.nav.omsorgspenger.journalforing.JournalforingMediator

fun main() {
    val applicationContext = ApplicationContext.Buider().build()
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
    /*
    routing {
        HealthRoute(healthService = applicationContext.healthService)
    }
     */
}

internal class ApplicationContext(
    internal val env: Environment,
    internal val serviceUser: ServiceUser,
    internal val httpClient: HttpClient,
    internal val stsRestClient: StsRestClient,
    internal val joarkClient: JoarkClient,
    internal val journalforingMediator: JournalforingMediator) {

    internal fun start() {}
    internal fun stop() {}

    internal class Buider(
        internal var env: Environment? = null,
        internal var serviceUser: ServiceUser? = null,
        internal var httpClient: HttpClient? = null,
        internal var stsRestClient: StsRestClient? = null,
        internal var joarkClient: JoarkClient? = null,
        internal var journalforingMediator: JournalforingMediator? = null) {
        internal fun build() : ApplicationContext {
            val benyttetEnv = env?:System.getenv()
            val benyttetHttpClient = httpClient?:HttpClient {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            }
            val benyttetServiceUser = serviceUser?:serviceUser?:readServiceUserCredentials()
            val benyttetStsRestClient = stsRestClient?:StsRestClient(
                env = benyttetEnv,
                serviceUser = benyttetServiceUser,
                httpClient = benyttetHttpClient
            )
            val benyttetJoarkClient = joarkClient?: JoarkClient(
                env = benyttetEnv,
                stsRestClient = benyttetStsRestClient,
                httpClient = benyttetHttpClient
            )

            return ApplicationContext(
                env = benyttetEnv,
                serviceUser = benyttetServiceUser,
                httpClient = benyttetHttpClient,
                stsRestClient = benyttetStsRestClient,
                joarkClient = benyttetJoarkClient,
                journalforingMediator = journalforingMediator?: JournalforingMediator(
                    joarkClient = benyttetJoarkClient
                )
            )
        }

        private companion object {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())
        }
    }
}