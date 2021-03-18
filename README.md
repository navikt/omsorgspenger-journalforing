omsorgspenger-journalforing
================

Tjänsten bygger på <a href="https://github.com/navikt/k9-rapid">k9-rapid</a> och har tre rivers i bruk som löser behov från omsorgspenger-rammemeldinger.


<a href="https://github.com/navikt/omsorgspenger-journalforing/tree/master/src/main/kotlin/no/nav/omsorgspenger/journalforing/FerdigstillJournalforing.kt">Ferdigstilljournalforing</a>
tar emot och löser behovet
<a href="https://github.com/navikt/omsorgspenger-rammemeldinger/blob/master/src/main/kotlin/no/nav/omsorgspenger/rivers/meldinger/FerdigstillJournalf%C3%B8ringForOmsorgspengerMelding.kt">FerdigstillJournalføringForOmsorgspenger</a><br>
Sänder en request till joark med identitetsnummer, saksnummer och journalpostId för att førsta uppdatera och sedan stänga journalposten.

<a href="https://github.com/navikt/omsorgspenger-journalforing/blob/master/src/main/kotlin/no/nav/omsorgspenger/oppgave/InitierGosysJournalf%C3%B8ringsoppgaver.kt">InitierGosysJournalføringsoppgaver</a>
tar emot behovet
<a href="https://github.com/navikt/omsorgspenger-rammemeldinger/blob/master/src/main/kotlin/no/nav/omsorgspenger/rivers/meldinger/OpprettGosysJournalf%C3%B8ringsoppgaverMelding.kt">OpprettGosysJournalføringsoppgaver</a> 
om det inte finns ett aktørId i løsningar.<br>
Finns det inte lägger den på ett HentPersonopplysninger-behov med aktørId som attribut och skickar tillbaka till rapiden.

<a href="https://github.com/navikt/omsorgspenger-journalforing/blob/master/src/main/kotlin/no/nav/omsorgspenger/oppgave/OpprettGosysJournalf%C3%B8ringsoppgaver.kt">OpprettGosysJournalføringsoppgaver</a>
löser behovet
<a href="https://github.com/navikt/omsorgspenger-rammemeldinger/blob/master/src/main/kotlin/no/nav/omsorgspenger/rivers/meldinger/OpprettGosysJournalf%C3%B8ringsoppgaverMelding.kt">OpprettGosysJournalføringsoppgaver</a><br>
Samlar ihop journalpostid, journalpostype, aktørId, lägger på kompletterande data (f.eks existerande oppgaver). Sänder samlade information till oppgave-api för att upprätta gosys oppgave.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sif_omsorgspenger.

Dokumentation på tjenester i bruk:<br>
<a href="https://confluence.adeo.no/display/BOA/Utviklerdokumentasjon+-+start+her">Joark</a><br>
<a href="https://confluence.adeo.no/display/TO/Systemdokumentasjon+Oppgave#">Oppgave</a>


