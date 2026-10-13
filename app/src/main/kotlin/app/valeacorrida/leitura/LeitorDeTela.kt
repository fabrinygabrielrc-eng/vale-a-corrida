package app.valeacorrida.leitura

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import app.valeacorrida.nucleo.Fragmento

/** Recolhe o texto que o aplicativo de corrida entrega à acessibilidade do Android. */
object LeitorDeTela {
    private const val PROFUNDIDADE_MAXIMA = 60
    private const val FRAGMENTOS_NO_MAXIMO = 600

    /**
     * `area` é o pedaço da tela ocupado pelo aplicativo; nulo quando o Android não informa.
     * `janelas` são as do aplicativo de corrida e `naTela` todas as que o Android listou: servem
     * ao rastro, para saber se o aplicativo estava mesmo à vista da acessibilidade.
     */
    class Tela(val fragmentos: List<Fragmento>, val area: Rect?, val janelas: Int = 0, val naTela: Int = 0)

    fun ler(servico: AccessibilityService, pacote: String): Tela {
        val raizes = mutableListOf<AccessibilityNodeInfo>()
        var area: Rect? = null
        var naTela = 0
        try {
            // A oferta pode vir numa janela à parte, por cima de outro aplicativo.
            for (janela in servico.windows) {
                naTela++
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
        return Tela(raizes.flatMap(::fragmentos), area, raizes.size, naTela)
    }

    /** Qual dos aplicativos de `pacotes` tem janela na tela agora, e quantas janelas o Android listou. */
    fun aVista(servico: AccessibilityService, pacotes: Set<String>): Pair<String?, Int> {
        var naTela = 0
        var achado: String? = null
        try {
            for (janela in servico.windows) {
                naTela++
                val pacote = janela.root?.packageName?.toString() ?: continue
                if (achado == null && pacote in pacotes) achado = pacote
            }
        } catch (_: Exception) {
            // Sem a lista de janelas, só dá para dizer que não achou.
        }
        return achado to naTela
    }

    /**
     * As janelas que o Android lista, para o registro: o tipo, de quem é e quantos filhos a raiz
     * tem. O título só entra na janela que vem sem raiz, que é a que o Android esconde do serviço.
     */
    fun janelas(servico: AccessibilityService): String = try {
        servico.windows.joinToString("; ") { janela ->
            val raiz = janela.root
            val dono = raiz?.let { "${it.packageName}, ${it.childCount} filhos" } ?: "sem raiz, título \"${janela.title ?: ""}\""
            "tipo ${janela.type}${if (janela.isActive) " ativa" else ""}: $dono"
        }.ifEmpty { "nenhuma" }
    } catch (e: Exception) {
        "não deu para listar (${e.javaClass.simpleName})"
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
