package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.JacksonSerializer
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.omsorgspenger.config.Environment
import no.nav.omsorgspenger.config.readServiceUserCredentials
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing
import no.nav.omsorgspenger.journalforing.JournalforingMediator

internal val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())

fun main() {
    RapidApplication.create(System.getenv()).apply {
        medAlleRivers()
    }.start()

}

internal fun RapidsConnection.medAlleRivers(
        env: Environment = System.getenv()) {

    val serviceUser = readServiceUserCredentials()
    val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }
    val stsRestClient = StsRestClient(env, serviceUser, httpClient)
    val joarkClient = JoarkClient(env, stsRestClient, httpClient)
    val journalforingMediator = JournalforingMediator(joarkClient)

    RapidApplication.create(env).apply {
        FerdigstillJournalforing(this, journalforingMediator)
    }.start()

}