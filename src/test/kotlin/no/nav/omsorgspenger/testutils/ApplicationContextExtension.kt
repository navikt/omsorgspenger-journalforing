package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.ApplicationContext
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class ApplicationContextExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        private val wireMockServer = WireMockBuilder()
            .withNaisStsSupport()
            .build()
            .stubJournalpostApi()

        private val applicationContextBuilder = ApplicationContext.Buider(
            env = mapOf(
                "JOARK_BASE_URL" to wireMockServer.journalpostApiBaseUrl(),
                "JOARK_API_GW_KEY" to "testApiKeyJoark",
                "STS_TOKEN_ENDPOINT" to wireMockServer.getNaisStsTokenUrl(),
                "STS_API_GW_KEY" to "testApiKeySts"
            ),
            serviceUser = ServiceUser(
                username = "foo",
                password = "bar"
            )
        )

        private val applicationContext = applicationContextBuilder.build()

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                wireMockServer.stop()
            })
        }

        private val støttedeParametre = listOf(
            ApplicationContext.Buider::class.java,
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
            ApplicationContext.Buider::class.java -> applicationContextBuilder
            else -> wireMockServer
        }
    }
}