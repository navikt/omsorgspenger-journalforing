package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.util.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.TimeUnit

internal class TestApplicationEngineExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        private val mockedEnvironment = MockedEnvironment(wireMockPort = 8084).start()

        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine(createTestEnvironment{
            config = getConfig(mockedEnvironment.appConfig)
        })

        init {
            testApplicationEngine.start(wait = true)
            Runtime.getRuntime().addShutdownHook(Thread {
                testApplicationEngine.stop(10, 60, TimeUnit.SECONDS)
                mockedEnvironment.stop()
            })
        }

    }
    @KtorExperimentalAPI
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return true
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return mockedEnvironment.wireMockServer
    }
}

@KtorExperimentalAPI
private fun getConfig(config: MutableMap<String,String>): ApplicationConfig {
    val fileConfig = ConfigFactory.load()
    val testConfig = ConfigFactory.parseMap(config)
    val mergedConfig = testConfig.withFallback(fileConfig)
    return HoconApplicationConfig(mergedConfig)
}