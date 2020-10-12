package no.nav.omsorgspenger.config

internal fun getStsApiKey(): String {
    var stsApiKey = System.getenv("STS_API_GW_KEY")?: "Test"
    return stsApiKey
}

internal fun getJoarkApiKey(): String {
    var joarkApiKey = System.getenv("JOARK_API_GW_KEY")?: "Test"
    return joarkApiKey
}