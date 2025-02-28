package no.nav.omsorgspenger.testutils.rapid

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.k9.rapid.behov.Behovsformat
import org.json.JSONObject

internal object TestRapidVerktøy {
    internal fun TestRapid.sisteMelding() =
        inspektør.message(inspektør.size - 1).toString()

    internal fun String.somJsonMessage() =
        JsonMessage(toString(), MessageProblems(this), SimpleMeterRegistry()).also { it.interestedIn("@løsninger") }

    internal fun TestRapid.sisteMeldingSomJsonMessage() =
        sisteMelding().somJsonMessage()

    internal fun TestRapid.sisteMeldingSomJSONObject() =
        JSONObject(sisteMelding())

    internal fun TestRapid.printSisteMelding() =
        println(sisteMeldingSomJSONObject().toString(1))

    internal fun TestRapid.løsningPå(behov: String) = sisteMeldingSomJSONObject().let { it.getJSONObject("@løsninger").getJSONObject(behov) }

    internal fun TestRapid.behov() = sisteMeldingSomJSONObject().getJSONArray(Behovsformat.Behovsrekkefølge).map { "$it" }.toSet()

    internal fun TestRapid.løsninger() = sisteMeldingSomJSONObject().let { when (it.has("@løsninger")) {
        true -> it.getJSONObject("@løsninger").keySet()
        false -> emptySet<String>()
    }}

    internal fun TestRapid.sisteMeldingErKlarForArkivering() {
        require(behov() == løsninger()) {
            "Behov=[${behov()}], Løsninger=[${løsninger()}]"
        }
    }
}