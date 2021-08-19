package no.nav.omsorgspenger.testutils.rapid

import com.fasterxml.jackson.databind.node.TextNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.k9.rapid.behov.Behovsformat
import org.json.JSONObject
import java.time.ZonedDateTime

internal object TestRapidVerktøy {
    internal fun TestRapid.sisteMelding() =
        inspektør.message(inspektør.size - 1).toString()

    internal fun String.somJsonMessage() =
        JsonMessage(toString(), MessageProblems(this)).also { it.interestedIn("@løsninger") }

    internal fun TestRapid.sisteMeldingSomJsonMessage() =
        sisteMelding().somJsonMessage()

    internal fun TestRapid.sisteMeldingSomJSONObject() =
        JSONObject(sisteMelding())

    internal fun TestRapid.printSisteMelding() =
        println(sisteMeldingSomJSONObject().toString(1))

    internal fun TestRapid.sisteMeldingHarLøsningPå(behov: String) {
        val key = "@løsninger.$behov.løst"
        val jsonMessage = sisteMeldingSomJsonMessage().also { it.interestedIn(key) }
        val node = jsonMessage[key]
        require(node is TextNode && ZonedDateTime.parse(node.textValue()) != null)
    }

    internal fun TestRapid.sisteMeldingManglerLøsningPå(behov: String) {
        val key = "@løsninger.$behov.løst"
        val jsonMessage = sisteMeldingSomJsonMessage().also { it.interestedIn(key) }
        val node = jsonMessage[key]
        require(node.isMissingOrNull())
    }

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