package no.nav.omsorgspenger

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

private object Metrics {

    val logger = LoggerFactory.getLogger(Metrics::class.java)

    val mottattBehov: Counter = Counter
            .build("omsorgspenger_behov_mottatt_total", "Antal behov mottatt")
            .labelNames("behov")
            .register()

    val feilBehovBehandling: Counter = Counter
            .build("omsorgspenger_behov_feil_total", "Antal feil vid behandling av behov")
            .labelNames("behov")
            .register()

    val behovBehandlet: Counter = Counter
            .build("omsorgspenger_behov_behandlet_total", "Antal lyckade behandlinger av behov")
            .labelNames("behov")
            .register()

}

private fun safeMetric(block: () -> Unit) = try {
    block()
} catch (cause: Throwable) {
    Metrics.logger.warn("Feil ved å rapportera metrics", cause)
}

internal fun incMottattBehov(behov: String) {
    safeMetric { Metrics.mottattBehov.labels(behov).inc() }
}

internal fun incBehandlingFeil(behov: String) {
    safeMetric { Metrics.feilBehovBehandling.labels(behov).inc() }
}

internal fun incBehandlingUtfort(behov: String) {
    safeMetric { Metrics.behovBehandlet.labels(behov).inc() }
}
