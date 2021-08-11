package no.nav.omsorgspenger.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.intellij.lang.annotations.Language
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object HtmlGenerator {
    internal fun genererHtml(
        tittel: String,
        farge: String,
        json: ObjectNode) : String {
        @Language("HTML")
        val html = """
            <html lang="no">
                <head>
                    <title>$tittel</title>
                    <style>
                        @page {
                            size: A4 portrait;
                            margin: 0.3cm 0.3cm 0.3cm;
                            padding-bottom: 1cm;
                        }
                        body { font-family: Arial, sans-serif; }
                        .json_object { margin:10px; padding-left:10px; border-left:1px solid #ccc }
                        .json_key { font-weight: bold; }
                        #header {
                            font-size: 18px;
                            font-weight: bold;
                            display: block;
                            color: #000;
                            padding: .4cm 0.7cm .4cm 2.2cm;
                            background: $farge url(data:image/jpeg;base64,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) no-repeat .7cm center;
                            background-size: 1.2cm;
                        }
                        #json {
                            padding-top: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div id="header">$tittel</div>
                    <div id="json">${json.toHtmlDiv()}</div>
                </body>
            </html>
        """.trimIndent().replace("\n", "").replace("  ", "")
        return html
    }

    private fun JsonNode.toHtmlDiv() : String {
        var html = ""

        when (this) {
            is ObjectNode -> {
                html += """<div class="json_object">"""
                fields().forEach { (navn, jsonNode) -> if (jsonNode !is NullNode) {
                    html += """<div><span class="json_key">${navn.prettyKey()}</span>: ${jsonNode.toHtmlDiv()}</div>"""
                }}
                html += "</div>"
            }
            is ArrayNode -> forEachIndexed { index, arrayElement ->
                html += arrayElement.toHtmlDiv()
                val erSisteElementIListen = size() == index + 1
                if (!arrayElement.isObject && !erSisteElementIListen) {
                    html += ", "
                }
            }
            else -> html += prettyValue()
        }
        return html
    }

    private fun JsonNode.prettyValue() : String = when (this) {
        is BooleanNode -> when (this.booleanValue()) {
            true -> "Ja"
            false -> "Nei"
        }
        else -> {
            val textValue = asText()
            parseOrNull { ZonedDateTime.parse(textValue).format(DATE_TIME_FORMATTER) }
                ?: parseOrNull { LocalDate.parse(textValue).format(DATE_FORMATTER) }
                ?: textValue.formatDurationOrNull()
                ?: textValue.formatPeriodeOrNull()
                ?: textValue
        }
    }

    private fun String.formatDurationOrNull() = parseOrNull { Duration.parse(this).let {
        "${it.toDaysPart().tidspunkt("dag")}${it.toHoursPart().tidspunkt("time", "timer")}${it.toMinutesPart().tidspunkt("minutt")}${it.toSecondsPart().tidspunkt("sekund")}".removeSuffix(", ")
    }}

    private fun String.formatPeriodeOrNull() = parseOrNull { when (this.matches(PERIODE_REGEX)) {
        true -> {
            val split = this.split("/")
            val fom = LocalDate.parse(split[0])
            val tom = LocalDate.parse(split[1])
            when (fom == tom) {
                true -> "Enkeltdagen ${fom.format(DATE_FORMATTER)}"
                false -> "Fra og med ${fom.format(DATE_FORMATTER)}, til og med ${tom.format(DATE_FORMATTER)}"
            }
        }
        false -> null
    }}

    private fun String.prettyKey() = formatPeriodeOrNull() ?: this

    private fun parseOrNull(parse: () -> String?) = kotlin.runCatching { parse() }.fold(
        onSuccess = { it },
        onFailure = { null }
    )

    private fun Number.tidspunkt(entall: String, flertall: String = "${entall}er") = when (this.toLong()) {
        0L -> ""
        1L -> "$this $entall, "
        else -> "$this $flertall, "
    }

    private val ZONE_ID = ZoneId.of("Europe/Oslo")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZONE_ID)
    private val PERIODE_REGEX = "\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}".toRegex()
}