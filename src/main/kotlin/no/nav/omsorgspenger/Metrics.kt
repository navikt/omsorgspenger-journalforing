package no.nav.omsorgspenger

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

private object SakMetrics {

    val logger = LoggerFactory.getLogger(SakMetrics::class.java)

    val mottattBehov: Counter = Counter
            .build("mottatt_behov", "Mottatt behov")
            .register()

    val feilBehovBehandling: Counter = Counter
            .build("behandlings_feil", "feil vid behandling av behov")
            .register()

    val behovBehandlet: Counter = Counter
            .build("behandling_utfort", "Lyckad behandling av behov")
            .register()

}

private fun safeMetric(block: () -> Unit) = try {
    block()
} catch (cause: Throwable) {
    SakMetrics.logger.warn("Feil ved å rapportera metrics", cause)
}

internal fun incMottattBehov() {
    safeMetric { SakMetrics.mottattBehov.inc() }
}

internal fun incBehandlingFeil() {
    safeMetric { SakMetrics.feilBehovBehandling.inc() }
}

internal fun incBehandlingUtfort() {
    safeMetric { SakMetrics.behovBehandlet.inc() }
}
