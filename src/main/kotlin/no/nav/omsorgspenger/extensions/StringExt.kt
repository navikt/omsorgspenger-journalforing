package no.nav.omsorgspenger.extensions

import org.json.JSONObject

internal object StringExt {
    internal fun String.trimJson() = JSONObject(this).toString()
}

