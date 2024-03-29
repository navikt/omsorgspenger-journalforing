package no.nav.omsorgspenger.oppgave

internal object OppgaveAttributter {
    private val støttedeJournalføringstyper = mapOf(
        "OverføreOmsorgsdager" to Attributter(
            behandlingstype = Behandlingstype.Overføring.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        ),
        "FordeleOmsorgsdager" to Attributter(
            behandlingstype = Behandlingstype.Overføring.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        ),
        "OverføreKoronaOmsorgsdager" to Attributter(
            behandlingstype = Behandlingstype.Overføring.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        ),
        "PleiepengerSyktBarn" to Attributter(
            behandlingstype = Behandlingstype.DigitalSøknad.kodeverdi,
            behandlingstema = Behandlingstema.PleiepengerSyktBarn.kodeverdi
        ),
        "UtbetaleOmsorgspenger" to Attributter(
            behandlingstype = Behandlingstype.Utbetaling.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        ),
        "KroniskSyktBarn" to Attributter(
            behandlingstype = Behandlingstype.DigitalSøknad.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        ),
        "MidlertidigAlene" to Attributter(
            behandlingstype = Behandlingstype.DigitalSøknad.kodeverdi,
            behandlingstema = Behandlingstema.Omsorgspenger.kodeverdi
        )
    )

    internal fun hentAttributer(journalføringstype: String) =
        støttedeJournalføringstyper[journalføringstype]
            ?: throw IllegalStateException("Støtter ikke journalføringstype $journalføringstype")

    internal data class Attributter(
        internal val behandlingstype: String,
        internal val behandlingstema: String
    )

    // https://kodeverk-web.nais.preprod.local/kodeverksoversikt/kodeverk/Behandlingstyper
    private enum class Behandlingstype(val kodeverdi: String) {
        DigitalSøknad("ae0227"),
        Overføring("ae0085"),
        Utbetaling("ae0007")
    }

    // https://kodeverk-web.nais.preprod.local/kodeverksoversikt/kodeverk/Behandlingstema
    private enum class Behandlingstema(val kodeverdi: String) {
        Omsorgspenger("ab0149"),
        PleiepengerSyktBarn("ab0320")
    }
}