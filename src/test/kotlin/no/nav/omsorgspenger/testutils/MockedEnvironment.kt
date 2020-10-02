package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.testutils.wiremock.journalpostApiBaseUrl
import no.nav.omsorgspenger.testutils.wiremock.stubJournalpostApi


internal class MockedEnvironment(
        wireMockPort: Int = 8082) {

    internal val wireMockServer = WireMockBuilder()
            .withPort(wireMockPort)
            .withAzureSupport()
            .withNaisStsSupport()
            .build()
            .stubJournalpostApi()

    internal val appConfig = mutableMapOf<String, String>()

    init {
        // Azure Issuers
        appConfig["nav.auth.issuers.0.type"] = "azure"
        appConfig["nav.auth.issuers.0.alias"] = "azure-v1"
        appConfig["nav.auth.issuers.0.discovery_endpoint"] = wireMockServer.getAzureV1WellKnownUrl()
        appConfig["nav.auth.issuers.0.audience"] = "omsorgspenger-journalforing"
        appConfig["nav.auth.issuers.0.azure.require_certificate_client_authentication"] = "false"
        appConfig["nav.auth.issuers.0.azure.required_roles"] = "access_as_application"

        appConfig["nav.auth.issuers.1.type"] = "azure"
        appConfig["nav.auth.issuers.1.alias"] = "azure-v2"
        appConfig["nav.auth.issuers.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
        appConfig["nav.auth.issuers.1.audience"] = "omsorgspenger-journalforing"
        appConfig["nav.auth.issuers.1.azure.require_certificate_client_authentication"] = "false"
        appConfig["nav.auth.issuers.1.azure.required_roles"] = "access_as_application"

        // Azure client
        appConfig["nav.auth.clients.0.alias"] = "azure-v2"
        appConfig["nav.auth.clients.0.client_id"] = "omsorgspenger-journalforing"
        appConfig["nav.auth.clients.0.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
        appConfig["nav.auth.clients.0.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()

        appConfig["nav.auth.clients.1.alias"] = "nais-sts"
        appConfig["nav.auth.clients.1.client_id"] = "omsorgspenger-journalforing"
        appConfig["nav.auth.clients.1.client_secret"] = "secret"
        appConfig["nav.auth.clients.1.token_endpoint"] = wireMockServer.getNaisStsTokenUrl()

        // Gateway URLS
        appConfig["nav.gateways.journalpostapi_base_url"] = wireMockServer.journalpostApiBaseUrl()
    }

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}