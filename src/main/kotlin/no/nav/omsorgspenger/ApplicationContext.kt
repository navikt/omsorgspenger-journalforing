package no.nav.omsorgspenger

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.k9.rapid.river.Environment
import no.nav.k9.rapid.river.RapidsStateListener
import no.nav.k9.rapid.river.csvTilSet
import no.nav.k9.rapid.river.hentRequiredEnv
import no.nav.omsorgspenger.joark.DokarkivClient
import no.nav.omsorgspenger.joark.SafGateway
import no.nav.omsorgspenger.oppgave.OppgaveClient
import java.net.URI

internal class ApplicationContext(
    internal val env: Environment,
    internal val dokarkivClient: DokarkivClient,
    internal val safGateway: SafGateway,
    internal val oppgaveClient: OppgaveClient,
    internal val healthChecks: Set<HealthCheck>
) {
    internal var rapidsState = RapidsStateListener.RapidsState.initialState()

    internal fun start() {}
    internal fun stop() {}

    internal class Builder(
        internal var env: Environment? = null,
        internal var httpClient: HttpClient? = null,
        internal var accessTokenClient: AccessTokenClient? = null,
        internal var dokarkivClient: DokarkivClient? = null,
        internal var safGateway: SafGateway? = null,
        internal var oppgaveClient: OppgaveClient? = null
    ) {
        internal fun build(): ApplicationContext {
            val benyttetEnv = env ?: System.getenv()
            val benyttetHttpClient = httpClient ?: HttpClient(OkHttp) {
                expectSuccess = false
            }
            val benyttetAccessTokenClient = accessTokenClient ?: ClientSecretAccessTokenClient(
                clientId = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_ID"),
                clientSecret = benyttetEnv.hentRequiredEnv("AZURE_APP_CLIENT_SECRET"),
                tokenEndpoint = URI(benyttetEnv.hentRequiredEnv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))
            )
            val benyttetDokarkivClient = dokarkivClient ?: DokarkivClient(
                accessTokenClient = benyttetAccessTokenClient,
                baseUrl = URI(benyttetEnv.hentRequiredEnv("DOKARKIV_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("DOKARKIV_SCOPES").csvTilSet()
            )
            val benyttetOppgaveClient = oppgaveClient ?: OppgaveClient(
                baseUrl = URI(benyttetEnv.hentRequiredEnv("OPPGAVE_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("OPPGAVE_SCOPES").csvTilSet(),
                accessTokenClient = benyttetAccessTokenClient,
                httpClient = benyttetHttpClient
            )
            val benyttetSafGateway = safGateway ?: SafGateway(
                accessTokenClient = benyttetAccessTokenClient,
                baseUrl = URI(benyttetEnv.hentRequiredEnv("SAF_BASE_URL")),
                scopes = benyttetEnv.hentRequiredEnv("SAF_SCOPES").csvTilSet()
            )

            return ApplicationContext(
                env = benyttetEnv,
                dokarkivClient = benyttetDokarkivClient,
                healthChecks = setOf(
                    benyttetDokarkivClient,
                    benyttetOppgaveClient,
                    benyttetSafGateway
                ),
                oppgaveClient = benyttetOppgaveClient,
                safGateway = benyttetSafGateway
            )
        }
    }
}