package no.nav.omsorgspenger.journalforjson

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.slf4j.Slf4jLogger
import com.openhtmltopdf.util.XRLog
import java.io.ByteArrayOutputStream


internal object PdfGenerator {

    init {
        XRLog.setLoggingEnabled(true)
        XRLog.setLoggerImpl(Slf4jLogger())
    }

    internal fun genererPdf(
        html: String
    ) = ByteArrayOutputStream().apply {
        Slf4jLogger()
        PdfRendererBuilder()
            // TODO: Laste fonter for å bruke PDFA_2_U
            //.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
            //.useFont()
            .withHtmlContent(html, null)
            .toStream(this)
            .run()
    }.toByteArray()
}