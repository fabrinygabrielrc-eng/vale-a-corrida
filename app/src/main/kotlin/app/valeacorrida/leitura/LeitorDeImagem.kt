package app.valeacorrida.leitura

import android.graphics.Bitmap
import app.valeacorrida.nucleo.Fragmento
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Reconhece o texto de uma imagem da tela, para quando o aplicativo de corrida não entrega
 * o texto à acessibilidade. O reconhecimento (ML Kit) roda no próprio aparelho, sem internet.
 */
class LeitorDeImagem {
    private val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Chama `aoTerminar` na thread principal; com nulo quando a leitura falha. */
    fun ler(imagem: Bitmap, aoTerminar: (List<Fragmento>?) -> Unit) {
        try {
            reconhecedor.process(InputImage.fromBitmap(imagem, 0))
                .addOnSuccessListener { texto -> aoTerminar(fragmentos(texto)) }
                .addOnFailureListener { aoTerminar(null) }
        } catch (_: Exception) {
            aoTerminar(null)
        }
    }

    fun fechar() = reconhecedor.close()

    private fun fragmentos(texto: Text): List<Fragmento> =
        texto.textBlocks.flatMap { it.lines }.mapNotNull { linha ->
            val caixa = linha.boundingBox ?: return@mapNotNull null
            Fragmento(linha.text, caixa.left, caixa.top, caixa.width(), caixa.height())
        }
}
