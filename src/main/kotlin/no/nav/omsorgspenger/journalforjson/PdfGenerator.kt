package no.nav.omsorgspenger.journalforjson

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream

internal object PdfGenerator {
    internal fun genererPdf(
        html: String
    ) = ByteArrayOutputStream().apply {
        PdfRendererBuilder()
            // TODO: Laste fonter for å bruke PDFA_2_U
            //.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
            //.useFont()
            .withHtmlContent(html, null)
            .toStream(this)
            .run()
    }.toByteArray()
}