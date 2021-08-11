package no.nav.omsorgspenger.journalforjson

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.k9.rapid.behov.Behov
import no.nav.k9.rapid.river.requireObject
import no.nav.omsorgspenger.Identitetsnummer

internal object HentNavnMelding {

    internal const val BehovNavn = "HentPersonopplysninger@journalførJson"

    internal fun behov(identitetsnummer: Identitetsnummer) = Behov(
        navn = BehovNavn,
        input = mapOf(
            "attributter" to listOf("navn"),
            "identitetsnummer" to listOf("$identitetsnummer"),
            "måFinneAllePersoner" to true
        )
    )

    internal fun validateLøsning(packet: JsonMessage) {
        packet.require(PersonopplysningerKey) { it.requireObject() }
    }

    internal fun hentLøsning(packet: JsonMessage, identitetsnummer: Identitetsnummer) =
        packet[identitetsnummer.sammensattNavnKey()].asText()

    private const val PersonopplysningerKey =  "@løsninger.${BehovNavn}.personopplysninger"
    private fun Identitetsnummer.sammensattNavnKey() = "$PersonopplysningerKey.$this.navn.sammensatt"
}