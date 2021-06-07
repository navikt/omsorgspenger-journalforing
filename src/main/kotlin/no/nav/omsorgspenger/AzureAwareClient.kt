package no.nav.omsorgspenger

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import java.net.URI

internal abstract class AzureAwareClient(
    private val navn: String,
    private val accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>,
    private val pingUrl: URI) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    protected fun authorizationHeader() =
        cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()

    override suspend fun check() =
        Result.merge(
            navn,
            accessTokenCheck(),
            pingCheck()
        )

    private fun accessTokenCheck() = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList()
            ?: emptyList()).contains("access_as_application")
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("AccessTokenCheck", "OK")
                false -> UnHealthy("AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("AccessTokenCheck", "Feil: ${it.message}") }
    )

    open suspend fun pingCheck() : Result = pingUrl.httpGet {
        it.header(HttpHeaders.Authorization, authorizationHeader())
    }.second.fold(
        onSuccess = { when (it.status.isSuccess()) {
            true -> Healthy("PingCheck", "OK: ${it.readText()}")
            false -> UnHealthy("PingCheck", "Feil: ${it.status}")
        }},
        onFailure = { UnHealthy("PingCheck", "Feil: ${it.message}") }
    )
}