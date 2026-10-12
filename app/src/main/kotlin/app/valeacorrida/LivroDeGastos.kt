package app.valeacorrida

import android.content.Context
import app.valeacorrida.nucleo.Categoria
import app.valeacorrida.nucleo.Gasto
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

/**
 * Os gastos que a pessoa anotou, num arquivo do próprio aparelho. Nada sai dele.
 *
 * A gravação é inteira ou nada: o texto vai para um arquivo ao lado e só então toma o lugar do
 * antigo, numa troca única. Se o celular desligar no meio, vale o arquivo de antes.
 */
class LivroDeGastos(private val arquivo: File) {
    constructor(contexto: Context) : this(File(contexto.filesDir, "gastos.json"))

    private val rascunho = File(arquivo.parentFile, arquivo.name + ".novo")
    private var gastos: MutableList<Gasto>? = null

    @Synchronized
    fun listar(): List<Gasto> = carregar().toList()

    /** Guarda o gasto e o devolve. Devolve nulo se não deu para gravar (aparelho sem espaço). */
    @Synchronized
    fun anotar(dia: LocalDate, categoria: Categoria, centavos: Long, nota: String = ""): Gasto? {
        val lista = carregar()
        val gasto = Gasto((lista.maxOfOrNull { it.id } ?: 0L) + 1, dia, categoria, centavos, nota.trim().take(NOTA_MAXIMA))
        if (!gravar(lista + gasto)) return null
        lista += gasto
        return gasto
    }

    @Synchronized
    fun apagar(id: Long) {
        val lista = carregar()
        val restantes = lista.filter { it.id != id }
        if (restantes.size != lista.size && gravar(restantes)) lista.removeAll { it.id == id }
    }

    private fun carregar(): MutableList<Gasto> {
        gastos?.let { return it }
        val lista = mutableListOf<Gasto>()
        try {
            val itens = JSONObject(arquivo.readText()).getJSONArray("gastos")
            for (i in 0 until itens.length()) {
                val item = itens.getJSONObject(i)
                lista += Gasto(
                    id = item.getLong("id"),
                    dia = LocalDate.parse(item.getString("dia")),
                    // Categoria que esta versão não conhece (arquivo de uma versão mais nova) não se perde.
                    categoria = Categoria.entries.firstOrNull { it.name == item.getString("categoria") } ?: Categoria.OUTROS,
                    centavos = item.getLong("centavos"),
                    nota = item.optString("nota"),
                )
            }
        } catch (_: FileNotFoundException) {
            // Ainda não há gasto anotado.
        } catch (_: Exception) {
            // Arquivo ilegível: guarda uma cópia antes de começar do zero. A próxima gravação
            // passaria por cima dele, e ali pode estar o mês inteiro de alguém.
            lista.clear()
            try {
                arquivo.copyTo(File(arquivo.parentFile, "gastos-ilegivel.json"), overwrite = true)
            } catch (_: Exception) {
            }
        }
        gastos = lista
        return lista
    }

    private fun gravar(lista: List<Gasto>): Boolean {
        val itens = JSONArray()
        for (gasto in lista) {
            itens.put(
                JSONObject()
                    .put("id", gasto.id)
                    .put("dia", gasto.dia.toString())
                    .put("categoria", gasto.categoria.name)
                    .put("centavos", gasto.centavos)
                    .put("nota", gasto.nota),
            )
        }
        val texto = JSONObject().put("versao", 1).put("gastos", itens).toString()
        return try {
            FileOutputStream(rascunho).use { saida ->
                saida.write(texto.toByteArray(Charsets.UTF_8))
                saida.fd.sync()
            }
            Files.move(rascunho.toPath(), arquivo.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            true
        } catch (_: Exception) {
            rascunho.delete()
            false
        }
    }

    private companion object {
        const val NOTA_MAXIMA = 80
    }
}
