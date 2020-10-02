package no.nav.omsorgspenger.journalforing

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

private object SakMetrics {

    val logger = LoggerFactory.getLogger(this::class.java)

    val mottattBehov: Counter = Counter
            .build("mottatt_behov", "Mottatt behov")
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
