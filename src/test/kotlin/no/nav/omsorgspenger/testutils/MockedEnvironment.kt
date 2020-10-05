package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi


internal class MockedEnvironment(
        wireMockPort: Int = 8082) {

    internal val wireMockServer = WireMockBuilder()
            .withPort(wireMockPort)
            .withNaisStsSupport()
            .build()
            .stubJournalpostApi()

    internal val appConfig = mutableMapOf<String, String>()

    init {
        appConfig["nav.auth.clients.1.alias"] = "nais-sts"
        appConfig["nav.auth.clients.1.client_id"] = "omsorgspenger-journalforing"
        appConfig["nav.auth.clients.1.client_secret"] = "secret"
        appConfig["nav.auth.clients.1.token_endpoint"] = wireMockServer.getNaisStsTokenUrl()

    }

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}