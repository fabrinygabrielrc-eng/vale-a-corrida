package app.valeacorrida.leitura

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import app.valeacorrida.nucleo.Fragmento

/** Recolhe o texto que o aplicativo de corrida entrega à acessibilidade do Android. */
object LeitorDeTela {
    private const val PROFUNDIDADE_MAXIMA = 60
    private const val FRAGMENTOS_NO_MAXIMO = 600

    /** `area` é o pedaço da tela ocupado pelo aplicativo; nulo quando o Android não informa. */
    class Tela(val fragmentos: List<Fragmento>, val area: Rect?)

    fun ler(servico: AccessibilityService, pacote: String): Tela {
        val raizes = mutableListOf<AccessibilityNodeInfo>()
        var area: Rect? = null
        try {
            // A oferta pode vir numa janela à parte, por cima de outro aplicativo.
            for (janela in servico.windows) {
                val raiz = janela.root ?: continue
                if (raiz.packageName?.toString() != pacote) continue
                raizes += raiz
                val limites = Rect().also(janela::getBoundsInScreen)
                area = area?.apply { union(limites) } ?: limites
            }
        } catch (_: Exception) {
            // Alguns aparelhos falham ao listar as janelas; a janela ativa resolve.
        }
        if (raizes.isEmpty()) {
            servico.rootInActiveWindow?.takeIf { it.packageName?.toString() == pacote }?.let(raizes::add)
        }
        return Tela(raizes.flatMap(::fragmentos), area)
    }

    fun fragmentos(raiz: AccessibilityNodeInfo): List<Fragmento> =
        mutableListOf<Fragmento>().also { coletar(raiz, it, 0) }

    private fun coletar(no: AccessibilityNodeInfo, saida: MutableList<Fragmento>, nivel: Int) {
        if (nivel > PROFUNDIDADE_MAXIMA || saida.size >= FRAGMENTOS_NO_MAXIMO || !no.isVisibleToUser) return
        val filhos = no.childCount
        // A descrição de um grupo costuma repetir o texto dos filhos: só vale em quem não tem filhos.
        val texto = no.text?.toString() ?: no.contentDescription?.toString()?.takeIf { filhos == 0 }
        if (!texto.isNullOrBlank()) {
            val limites = Rect().also(no::getBoundsInScreen)
            saida += Fragmento(texto, limites.left, limites.top, limites.width(), limites.height())
        }
        for (i in 0 until filhos) {
            val filho = try { no.getChild(i) } catch (_: Exception) { null } ?: continue
            coletar(filho, saida, nivel + 1)
        }
    }
}
