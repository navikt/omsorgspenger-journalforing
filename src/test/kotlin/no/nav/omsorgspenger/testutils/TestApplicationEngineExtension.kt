package no.nav.omsorgspenger.testutils

import io.ktor.server.engine.stop
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.KtorExperimentalAPI
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class TestApplicationEngineExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        private val mockedEnvironment = MockedEnvironment(wireMockPort = 8084).start()

        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine()

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