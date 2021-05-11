package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.oppgaveApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi
import no.nav.omsorgspenger.testutils.wiremock.stubOppgaveMock
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
                .stubOppgaveMock()

        private val applicationContextBuilder = ApplicationContext.Builder(
                env = mapOf(
                    "JOARK_BASE_URL" to wireMockServer.journalpostApiBaseUrl(),
                    "DOKARKIV_SCOPES" to "testScope/.default",
                    "OPPGAVE_BASE_URL" to wireMockServer.oppgaveApiBaseUrl(),
                    "OPPGAVE_SCOPES" to "test/.default",
                    "AZURE_APP_CLIENT_ID" to "omsorgspenger-journalforing",
                    "AZURE_APP_CLIENT_SECRET" to "azureSecret",
                    "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to wireMockServer.getAzureV2TokenUrl()
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