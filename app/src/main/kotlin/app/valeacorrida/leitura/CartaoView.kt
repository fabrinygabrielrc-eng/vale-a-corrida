package app.valeacorrida.leitura

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import app.valeacorrida.R
import app.valeacorrida.Textos
import app.valeacorrida.nucleo.Avaliacao
import app.valeacorrida.nucleo.Veredito

/**
 * O cartão que aparece por cima da oferta: o veredito em palavra e em cor, quanto a corrida
 * paga por km e por hora, e a distância total. Feito para ser lido de relance, com o carro parado.
 *
 * Tem cara de placa de estrada: cor cheia, filete por dentro e a letra das placas.
 */
class CartaoView(contexto: Context) : LinearLayout(contexto) {
    private val fundo = GradientDrawable()
    private val filete = GradientDrawable()
    private val titulo = texto(14f, R.font.overpass_semibold)
    private val porKm = texto(26f, R.font.overpass_extrabold)
    private val porHora = texto(26f, R.font.overpass_extrabold)
    private val detalhe = texto(14f, R.font.overpass_regular)

    init {
        orientation = VERTICAL
        fundo.cornerRadius = dp(16f)
        filete.cornerRadius = dp(12f)
        filete.setColor(Color.TRANSPARENT)
        val recuo = dp(4f).toInt()
        background = LayerDrawable(arrayOf(fundo, filete)).apply { setLayerInset(1, recuo, recuo, recuo, recuo) }
        setPadding(dp(18f).toInt(), dp(13f).toInt(), dp(18f).toInt(), dp(13f).toInt())

        val numeros = LinearLayout(contexto).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(porKm)
            addView(porHora, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(18f).toInt()
            })
        }
        addView(titulo)
        addView(numeros, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(2f).toInt()
        })
        addView(detalhe)
    }

    fun mostrar(avaliacao: Avaliacao) {
        val (corDoFundo, corDoTexto) = cores(avaliacao.veredito)
        fundo.setColor(corDoFundo)
        // O contorno escuro separa o cartão de um mapa claro ou escuro.
        fundo.setStroke(dp(1.5f).toInt(), Color.argb(90, 0, 0, 0))
        filete.setStroke(dp(1.5f).toInt(), corDoTexto and 0x00FFFFFF or (0xB3 shl 24))
        for (campo in listOf(titulo, porKm, porHora, detalhe)) campo.setTextColor(corDoTexto)

        titulo.text = Textos.titulo(avaliacao)
        porKm.text = Textos.porKm(avaliacao)
        porHora.text = Textos.porHora(avaliacao)
        porHora.visibility = if (avaliacao.porHora == null) GONE else VISIBLE
        detalhe.text = Textos.detalhe(avaliacao)
        contentDescription = "${titulo.text}. ${porKm.text}. ${porHora.text}. ${detalhe.text}"
    }

    private fun texto(tamanho: Float, recursoDaLetra: Int) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanho)
        typeface = letra(recursoDaLetra)
        fontFeatureSettings = "tnum" // algarismos da mesma largura: o número não dança ao mudar
        includeFontPadding = false
        maxLines = 1
    }

    /** A letra das placas. Se o aparelho não conseguir carregá-la, o cartão sai com a letra do sistema. */
    private fun letra(recurso: Int): Typeface = try {
        resources.getFont(recurso)
    } catch (_: Exception) {
        Typeface.DEFAULT_BOLD
    }

    private fun dp(valor: Float): Float = valor * resources.displayMetrics.density

    companion object {
        /** Fundo e texto de cada veredito. O texto tem contraste de pelo menos 4,5:1 com o fundo. */
        fun cores(veredito: Veredito): Pair<Int, Int> = when (veredito) {
            Veredito.BOA -> Color.rgb(0x15, 0x7A, 0x40) to Color.WHITE
            Veredito.MEDIA -> Color.rgb(0xF2, 0xB7, 0x05) to Color.rgb(0x1A, 0x14, 0x00)
            Veredito.RUIM -> Color.rgb(0xC0, 0x2B, 0x33) to Color.WHITE
        }
    }
}
