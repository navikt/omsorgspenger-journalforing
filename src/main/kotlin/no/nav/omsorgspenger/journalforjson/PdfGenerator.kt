package no.nav.omsorgspenger.journalforjson

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import java.io.ByteArrayOutputStream

internal object PdfGenerator {

    private val colorProfile = PdfGenerator::class.java.getResourceAsStream("/pdf/sRGB2014.icc")!!.readBytes()
    
    init {
        XRLog.setLoggingEnabled(false)
    }

    internal fun genererPdf(
        html: String
    ) = ByteArrayOutputStream().apply {
        PdfRendererBuilder()
            // TODO: Laste fonter for å bruke PDFA_2_U
            //.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
            //.useFont()
            .withHtmlContent(html, null)
            .useColorProfile(colorProfile)
            .toStream(this)
            .run()
    }.toByteArray()
}