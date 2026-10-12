package app.valeacorrida.leitura

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import app.valeacorrida.Textos
import app.valeacorrida.nucleo.Avaliacao
import app.valeacorrida.nucleo.Veredito

/**
 * O cartão que aparece por cima da oferta: o veredito em palavra e em cor, quanto a corrida
 * paga por km e por hora, e a distância total. Feito para ser lido de relance, com o carro parado.
 */
class CartaoView(contexto: Context) : LinearLayout(contexto) {
    private val fundo = GradientDrawable()
    private val titulo = texto(14f, negrito = true)
    private val porKm = texto(27f, negrito = true)
    private val porHora = texto(27f, negrito = true)
    private val detalhe = texto(14f, negrito = false)

    init {
        orientation = VERTICAL
        fundo.cornerRadius = dp(18f)
        background = fundo
        setPadding(dp(18f).toInt(), dp(10f).toInt(), dp(18f).toInt(), dp(12f).toInt())

        val numeros = LinearLayout(contexto).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(porKm)
            addView(porHora, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(20f).toInt()
            })
        }
        addView(titulo)
        addView(numeros)
        addView(detalhe)
    }

    fun mostrar(avaliacao: Avaliacao) {
        val (corDoFundo, corDoTexto) = cores(avaliacao.veredito)
        fundo.setColor(corDoFundo)
        // O contorno escuro separa o cartão de um mapa claro ou escuro.
        fundo.setStroke(dp(1.5f).toInt(), Color.argb(90, 0, 0, 0))
        for (campo in listOf(titulo, porKm, porHora, detalhe)) campo.setTextColor(corDoTexto)

        titulo.text = Textos.titulo(avaliacao)
        porKm.text = Textos.porKm(avaliacao)
        porHora.text = Textos.porHora(avaliacao)
        porHora.visibility = if (avaliacao.porHora == null) GONE else VISIBLE
        detalhe.text = Textos.detalhe(avaliacao)
        contentDescription = "${titulo.text}. ${porKm.text}. ${porHora.text}. ${detalhe.text}"
    }

    private fun texto(tamanho: Float, negrito: Boolean) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanho)
        typeface = if (negrito) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        fontFeatureSettings = "tnum" // algarismos da mesma largura: o número não dança ao mudar
        includeFontPadding = false
        maxLines = 1
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
