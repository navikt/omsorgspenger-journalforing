package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi


internal class MockedEnvironment(
        wireMockPort: Int = 8082) {

    internal val wireMockServer = WireMockBuilder()
            .withPort(wireMockPort)
            .withNaisStsSupport()
            .build()
            .stubJournalpostApi()

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}