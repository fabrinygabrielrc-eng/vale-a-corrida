package app.valeacorrida.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.valeacorrida.nucleo.Categoria

/**
 * Os desenhos das placas de serviço: bomba, garfo e faca, gota. Cheios e simples, como os das
 * placas de estrada, para se reconhecerem num relance. Todos numa grade de 24 por 24.
 */
object Pictogramas {

    fun de(categoria: Categoria): ImageVector = when (categoria) {
        Categoria.COMBUSTIVEL -> Bomba
        Categoria.ALIMENTACAO -> GarfoEFaca
        Categoria.MANUTENCAO -> Icons.Filled.Build
        Categoria.LAVAGEM -> Gota
        Categoria.PEDAGIO -> Cancela
        Categoria.CARRO -> Carro
        Categoria.DOCUMENTOS -> Documento
        Categoria.CELULAR -> Celular
        Categoria.OUTROS -> TresPontos
    }

    val Bomba: ImageVector by lazy {
        pictograma("Bomba") {
            cheio(vazado = true) {
                retangulo(4.5f, 3f, 9f, 17f, 1.2f)
                retangulo(6.5f, 5.5f, 5f, 4.5f, 0.5f) // o visor
            }
            cheio { retangulo(3.5f, 19.5f, 11f, 1.8f, 0.4f) }
            risco(1.7f) { // a mangueira e o bico
                moveTo(13.5f, 9f)
                horizontalLineTo(15.3f)
                arcToRelative(1.6f, 1.6f, 0f, false, true, 1.6f, 1.6f)
                verticalLineTo(16.6f)
                arcToRelative(1.7f, 1.7f, 0f, false, false, 3.4f, 0f)
                verticalLineTo(9f)
                lineTo(17.8f, 6.3f)
            }
        }
    }

    val GarfoEFaca: ImageVector by lazy {
        pictograma("GarfoEFaca") {
            risco(1.7f) {
                moveTo(5.5f, 3f)
                verticalLineTo(8f)
                arcToRelative(2.75f, 2.75f, 0f, false, false, 5.5f, 0f)
                verticalLineTo(3f)
            }
            risco(1.7f) {
                moveTo(8.25f, 3f)
                verticalLineTo(21f)
            }
            cheio {
                moveTo(15.2f, 2.6f)
                curveTo(17.9f, 3.2f, 19.3f, 6.4f, 19.3f, 12.5f)
                horizontalLineTo(17.1f)
                verticalLineTo(21.8f)
                horizontalLineTo(15.2f)
                close()
            }
        }
    }

    val Gota: ImageVector by lazy {
        pictograma("Gota") {
            cheio {
                moveTo(12f, 2.8f)
                curveTo(12f, 2.8f, 5.8f, 9.8f, 5.8f, 14.6f)
                arcToRelative(6.2f, 6.2f, 0f, false, false, 12.4f, 0f)
                curveTo(18.2f, 9.8f, 12f, 2.8f, 12f, 2.8f)
                close()
            }
        }
    }

    val Cancela: ImageVector by lazy {
        pictograma("Cancela") {
            cheio { retangulo(4.2f, 10f, 3.4f, 10f, 0.5f) }
            cheio { retangulo(2.5f, 19.6f, 19f, 1.6f, 0.4f) }
            risco(2.8f) {
                moveTo(6f, 11.2f)
                lineTo(20.6f, 6.6f)
            }
            cheio { circulo(5.9f, 11.2f, 2.5f) } // o eixo da cancela
        }
    }

    val Carro: ImageVector by lazy {
        pictograma("Carro") {
            cheio(vazado = true) {
                moveTo(2.5f, 15.5f)
                verticalLineTo(12.3f)
                curveTo(2.5f, 11.6f, 3f, 11.1f, 3.7f, 10.9f)
                lineTo(6f, 10.3f)
                lineTo(8f, 6.6f)
                curveTo(8.3f, 6.1f, 8.8f, 5.8f, 9.4f, 5.8f)
                horizontalLineTo(14.9f)
                curveTo(15.5f, 5.8f, 16f, 6.1f, 16.3f, 6.5f)
                lineTo(18.6f, 10.2f)
                lineTo(20.4f, 10.8f)
                curveTo(21.1f, 11f, 21.5f, 11.6f, 21.5f, 12.3f)
                verticalLineTo(15.5f)
                close()
                // as janelas
                moveTo(9.5f, 7.5f)
                lineTo(8.1f, 10.2f)
                horizontalLineTo(16.5f)
                lineTo(14.9f, 7.5f)
                close()
            }
            cheio {
                circulo(7.2f, 15.9f, 2.3f)
                circulo(16.8f, 15.9f, 2.3f)
            }
        }
    }

    val Documento: ImageVector by lazy {
        pictograma("Documento") {
            cheio(vazado = true) {
                moveTo(6f, 2.8f)
                horizontalLineTo(14f)
                lineTo(18.5f, 7.3f)
                verticalLineTo(21.2f)
                horizontalLineTo(6f)
                close()
                retangulo(8.3f, 10.6f, 7.9f, 1.6f)
                retangulo(8.3f, 13.9f, 7.9f, 1.6f)
                retangulo(8.3f, 17.2f, 4.6f, 1.6f)
            }
        }
    }

    val Celular: ImageVector by lazy {
        pictograma("Celular") {
            cheio(vazado = true) {
                retangulo(7f, 2.5f, 10f, 19f, 1.8f)
                retangulo(8.7f, 5f, 6.6f, 12f, 0.3f) // a tela
            }
        }
    }

    val TresPontos: ImageVector by lazy {
        pictograma("TresPontos") {
            cheio {
                circulo(5.5f, 12f, 1.9f)
                circulo(12f, 12f, 1.9f)
                circulo(18.5f, 12f, 1.9f)
            }
        }
    }

    /** Dois botões de deslizar: os valores que a pessoa ajusta. */
    val Ajustes: ImageVector by lazy {
        pictograma("Ajustes") {
            risco(1.8f) {
                moveTo(4f, 8f)
                horizontalLineTo(20f)
                moveTo(4f, 16f)
                horizontalLineTo(20f)
            }
            cheio {
                circulo(9f, 8f, 2.9f)
                circulo(15f, 16f, 2.9f)
            }
        }
    }
}

private fun pictograma(nome: String, desenho: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply(desenho).build()

/** Forma cheia. Com `vazado`, o que é desenhado por dentro vira furo (o visor da bomba, a tela do celular). */
private fun ImageVector.Builder.cheio(vazado: Boolean = false, tracado: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        pathFillType = if (vazado) PathFillType.EvenOdd else PathFillType.NonZero,
        pathBuilder = tracado,
    )
}

private fun ImageVector.Builder.risco(largura: Float, tracado: PathBuilder.() -> Unit) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = largura,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = tracado,
    )
}

private fun PathBuilder.circulo(x: Float, y: Float, raio: Float) {
    moveTo(x - raio, y)
    arcToRelative(raio, raio, 0f, true, true, 2 * raio, 0f)
    arcToRelative(raio, raio, 0f, true, true, -2 * raio, 0f)
    close()
}

private fun PathBuilder.retangulo(x: Float, y: Float, largura: Float, altura: Float, raio: Float = 0f) {
    moveTo(x + raio, y)
    horizontalLineToRelative(largura - 2 * raio)
    if (raio > 0f) arcToRelative(raio, raio, 0f, false, true, raio, raio)
    verticalLineToRelative(altura - 2 * raio)
    if (raio > 0f) arcToRelative(raio, raio, 0f, false, true, -raio, raio)
    horizontalLineToRelative(-(largura - 2 * raio))
    if (raio > 0f) arcToRelative(raio, raio, 0f, false, true, -raio, -raio)
    verticalLineToRelative(-(altura - 2 * raio))
    if (raio > 0f) arcToRelative(raio, raio, 0f, false, true, raio, -raio)
    close()
}
