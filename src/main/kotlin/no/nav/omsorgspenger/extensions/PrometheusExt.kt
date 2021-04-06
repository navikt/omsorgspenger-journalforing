package no.nav.omsorgspenger.extensions

import io.prometheus.client.Counter

internal object PrometheusExt {
    internal fun Counter.ensureRegistered() = try {
        register()
    } catch (cause: IllegalArgumentException) {
        this
    }
}