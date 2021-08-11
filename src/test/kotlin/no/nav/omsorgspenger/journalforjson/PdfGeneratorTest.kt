package no.nav.omsorgspenger.journalforjson

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

internal class PdfGeneratorTest {

    @Test
    fun `Generere PDF av søknad om pleiepenger`() {
        pleiepengesøknad(lagrePdf = false)
    }

    @Test
    @Disabled
    fun `Opprette PDF av søknad om pleiepenger`() {
        pleiepengesøknad(lagrePdf = true)
    }

    private fun pleiepengesøknad(lagrePdf: Boolean) = PdfGenerator.genererPdf(
        html = HtmlGenerator.genererHtml(
            tittel = "Søknad om pleiepenger",
            farge = "#FC0FC0",
            json = pleiepengesøknad.somObjectNode()
        )
    ).also { pdfBytes -> if (lagrePdf) {
        File(pdfPath("pleiepengesøknad")).writeBytes(pdfBytes)
    }}

    private companion object {
        @Language("JSON")
        private val pleiepengesøknad = """
        {
          "søknadId": "1",
          "versjon": "2.0.0",
          "mottattDato": "2020-10-12T12:53:21.046Z",
          "søker": {
            "norskIdentitetsnummer": "11111111111"
          },
          "journalposter" : [ {
            "inneholderInfomasjonSomIkkeKanPunsjes" : false,
            "inneholderMedisinskeOpplysninger" : false,
            "journalpostId" : "sajhdasd83724234"
          } ],
          "ytelse": {
            "type" : "PLEIEPENGER_SYKT_BARN",
            "søknadsperiode" : [ "2018-12-30/2019-10-20" ],
            "endringsperiode" : [ ],
            "dataBruktTilUtledning" : {
              "harForståttRettigheterOgPlikter" : true,
              "harBekreftetOpplysninger" : true,
              "samtidigHjemme" : false,
              "harMedsøker" : false,
              "bekrefterPeriodeOver8Uker" : true
            },
            "infoFraPunsj" : {
              "søknadenInneholderInfomasjonSomIkkeKanPunsjes" : false,
              "inneholderMedisinskeOpplysninger" : false
            },
            "barn" : {
              "norskIdentitetsnummer" : "11111111111",
              "fødselsdato" : null
            },
            "arbeidAktivitet" : {
              "selvstendigNæringsdrivende" : [ {
                "perioder" : {
                  "2018-11-11/2018-11-30" : {
                    "virksomhetstyper" : [ "FISKE", "JORDBRUK" ]
                  }
                },
                "virksomhetNavn" : "Test"
              } ],
              "frilanser" : {
                "startdato" : "2019-10-10",
                "jobberFortsattSomFrilans" : true
              }
            },
            "beredskap" : {
              "perioder" : {
                "2019-02-21/2019-05-21" : {
                  "tilleggsinformasjon" : "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                },
                "2018-12-30/2019-02-20" : {
                  "tilleggsinformasjon" : "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                }
              }
            },
            "nattevåk" : {
              "perioder" : {
                "2019-02-21/2019-05-21" : {
                  "tilleggsinformasjon" : "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                },
                "2018-12-30/2019-02-20" : {
                  "tilleggsinformasjon" : "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
                }
              }
            },
            "tilsynsordning" : {
              "perioder" : {
                "2019-01-01/2019-01-01" : {
                  "etablertTilsynTimerPerDag" : "PT7H30M"
                },
                "2019-01-02/2019-01-02" : {
                  "etablertTilsynTimerPerDag" : "PT7H30M"
                },
                "2019-01-03/2019-01-09" : {
                  "etablertTilsynTimerPerDag" : "PT7H30M"
                }
              }
            },
            "arbeidstid" : {
              "arbeidstakerList" : [ {
                "norskIdentitetsnummer" : null,
                "organisasjonsnummer" : "999999999",
                "arbeidstidInfo" : {
                  "perioder" : {
                    "2018-12-30/2019-10-20" : {
                      "jobberNormaltTimerPerDag" : "PT7H30M",
                      "faktiskArbeidTimerPerDag" : "PT7H30M"
                    }
                  }
                }
              } ],
              "frilanserArbeidstidInfo" : null,
              "selvstendigNæringsdrivendeArbeidstidInfo" : null
            },
            "uttak" : {
              "perioder" : {
                "2018-12-30/2019-10-20" : {
                  "timerPleieAvBarnetPerDag" : "PT7H30M"
                }
              }
            },
            "omsorg" : {
              "relasjonTilBarnet" : "MOR",
              "beskrivelseAvOmsorgsrollen" : "Noe tilleggsinformasjon. Lorem ipsum æÆøØåÅ."
            },
            "lovbestemtFerie" : {
              "perioder" : {
                "2019-02-21/2019-10-20" : { }
              }
            },
            "bosteder" : {
              "perioder" : {
                "2018-12-30/2019-10-20" : {
                  "land" : "DNK"
                }
              }
            },
            "utenlandsopphold" : {
              "perioder" : {
                "2018-12-30/2019-10-20" : {
                  "land" : "DNK",
                  "årsak" : "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning"
                }
              }
            }
          }
        }
        """.trimIndent()
        private fun String.somObjectNode() = jacksonObjectMapper().readTree(this) as ObjectNode
        private fun pdfPath(id: String) = "${System.getProperty("user.dir")}/generated-pdf-$id.pdf"
    }
}