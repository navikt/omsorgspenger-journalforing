package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.json.*
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.omsorgspenger.journalforing.FerdigstillJournalforing
import org.slf4j.LoggerFactory

internal val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())

internal val secureLog = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val env = System.getenv()
    val serviceUser = readServiceUserCredentials()
    val stsRestClient = StsRestClient(requireNotNull(env["STS_URL"]), serviceUser)
    val httpClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
    }
    val joarkClient = JoarkClient(requireNotNull(env["JOARK_BASE_URL"]), stsRestClient, httpClient)

    RapidApplication.create(env).apply {
        FerdigstillJournalforing(this, joarkClient)
    }.start()
}