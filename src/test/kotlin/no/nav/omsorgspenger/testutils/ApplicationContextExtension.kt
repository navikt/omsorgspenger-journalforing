package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.util.KtorExperimentalAPI
import java.net.URI
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class ApplicationContextExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        private val wireMockServer = WireMockBuilder()
                .withAzureSupport()
                .build()
                .stubJournalpostApi()

        private val applicationContextBuilder = ApplicationContext.Builder(
                env = mapOf(
                        "JOARK_BASE_URL" to wireMockServer.journalpostApiBaseUrl(),
                        "DOKARKIV_SCOPES" to "testScope/.default"
                ),
                accessTokenClient =  ClientSecretAccessTokenClient(
                        clientId = "omsorgspenger-journalforing",
                        clientSecret = "azureSecret",
                        tokenEndpoint = URI(wireMockServer.getAzureV2TokenUrl())
                )
        )

        private val applicationContext = applicationContextBuilder.build()

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                wireMockServer.stop()
            })
        }

        private val støttedeParametre = listOf(
                ApplicationContext.Builder::class.java,
                ApplicationContext::class.java,
                WireMockServer::class.java
        )
    }

    @KtorExperimentalAPI
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            ApplicationContext::class.java -> applicationContext
            ApplicationContext.Builder::class.java -> applicationContextBuilder
            else -> wireMockServer
        }
    }
}