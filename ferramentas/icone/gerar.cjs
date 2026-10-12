// O ícone do Vale a Corrida: a estrada em forma de "certo", sobre o verde da corrida boa.
// Um desenho só gera os arquivos do Android (vetor) e as imagens de conferência (PNG).
//
// Uso, nesta pasta:
//   npm install @resvg/resvg-js
//   node gerar.cjs            desenha as imagens em ferramentas/icone/saida, para olhar
//   node gerar.cjs --gravar   grava também os arquivos do Android e docs/icone.png
const fs = require('fs')
const path = require('path')
const { Resvg } = require('@resvg/resvg-js')

const projeto = path.join(__dirname, '..', '..')
const saida = path.join(__dirname, 'saida')
fs.mkdirSync(saida, { recursive: true })

const VERDE = '#178A47'
const ASFALTO = '#15181C'
const FAIXA = '#F4F4EF'

// Na grade de 108 do ícone adaptável, tudo dentro do círculo seguro de raio 33.
const A = [34, 56], B = [47, 69], C = [74, 40]
const LARGURA = 14
const TRACO = 2.3

const n = (v) => Number(v.toFixed(2))
function tracos(de, ate, pedacos) {
  const dx = ate[0] - de[0], dy = ate[1] - de[1]
  const tamanho = Math.hypot(dx, dy)
  const ponto = (d) => [n(de[0] + (dx * d) / tamanho), n(de[1] + (dy * d) / tamanho)]
  return pedacos.map(([inicio, fim]) => [ponto(inicio), ponto(fim)])
}
const faixas = [
  ...tracos(A, B, [[2.5, 7.5], [11.5, 16]]),
  ...tracos(B, C, [[4, 9], [13.5, 18.5], [23, 28], [32.5, 37.5]]),
]
const estrada = `M${A[0]},${A[1]} L${B[0]},${B[1]} L${C[0]},${C[1]}`

// ---------- imagens de conferência
const desenho = `<path d="${estrada}" fill="none" stroke="${ASFALTO}" stroke-width="${LARGURA}" stroke-linecap="round" stroke-linejoin="round"/>` +
  faixas.map(([p, q]) => `<path d="M${p[0]},${p[1]} L${q[0]},${q[1]}" stroke="${FAIXA}" stroke-width="${TRACO}" stroke-linecap="round"/>`).join('')

// O Android corta o ícone de 108 numa máscara de 72 no centro. `corte` é a forma da máscara.
function icone(corte) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72"><defs><clipPath id="m">${corte}</clipPath></defs>
  <g clip-path="url(#m)"><rect x="0" y="0" width="108" height="108" fill="${VERDE}"/>${desenho}</g></svg>`
}
const redondo = icone('<circle cx="54" cy="54" r="36"/>')
const quadrado = icone('<rect x="18" y="18" width="72" height="72" rx="16"/>')

function png(svg, lado, arquivo) {
  fs.writeFileSync(arquivo, new Resvg(svg, { fitTo: { mode: 'width', value: lado } }).render().asPng())
}
png(quadrado, 512, path.join(saida, 'icone-512.png'))

// Uma folha com os tamanhos em que o ícone aparece de verdade, em fundo claro e escuro.
const dados = (svg) => 'data:image/svg+xml;base64,' + Buffer.from(svg).toString('base64')
let folha = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="760" height="420">
<rect width="760" height="210" fill="#E9ECEF"/><rect y="210" width="760" height="210" fill="#101214"/>`
for (const y of [0, 210]) {
  let x = 24
  for (const [svg, lado] of [[quadrado, 160], [redondo, 160], [redondo, 96], [quadrado, 64], [redondo, 48], [redondo, 32]]) {
    folha += `<image x="${x}" y="${y + (210 - lado) / 2}" width="${lado}" height="${lado}" xlink:href="${dados(svg)}"/>`
    x += lado + 28
  }
}
png(folha + '</svg>', 760, path.join(saida, 'folha.png'))
console.log('imagens de conferência em ' + saida)

// ---------- Android
if (process.argv.includes('--gravar')) {
  const cabecalho = (comentario) => `<?xml version="1.0" encoding="utf-8"?>
<!-- ${comentario} -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
`
  const risco = (d, cor, largura) => `    <path
        android:pathData="${d}"
        android:strokeColor="${cor}"
        android:strokeWidth="${largura}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
`
  const frente = cabecalho('A estrada em forma de "certo": a corrida que vale. Gerado por ferramentas/icone/gerar.cjs; a faixa do meio é traço por traço porque o vetor do Android não sabe tracejar.') +
    risco(estrada, ASFALTO, LARGURA) +
    faixas.map(([p, q]) => risco(`M${p[0]},${p[1]} L${q[0]},${q[1]}`, FAIXA, TRACO)).join('') + '</vector>\n'
  const umaCor = cabecalho('O mesmo desenho numa cor só, para o Android pintar com a cor do tema do aparelho. Gerado por ferramentas/icone/gerar.cjs.') +
    risco(estrada, '#FFFFFF', LARGURA) + '</vector>\n'
  const adaptavel = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/verde_da_marca" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`
  const res = path.join(projeto, 'app', 'src', 'main', 'res')
  fs.writeFileSync(path.join(res, 'drawable', 'ic_launcher_foreground.xml'), frente)
  fs.writeFileSync(path.join(res, 'drawable', 'ic_launcher_monochrome.xml'), umaCor)
  fs.mkdirSync(path.join(res, 'mipmap-anydpi'), { recursive: true })
  fs.writeFileSync(path.join(res, 'mipmap-anydpi', 'ic_launcher.xml'), adaptavel)
  fs.writeFileSync(path.join(res, 'mipmap-anydpi', 'ic_launcher_round.xml'), adaptavel)
  fs.copyFileSync(path.join(saida, 'icone-512.png'), path.join(projeto, 'docs', 'icone.png'))
  console.log('arquivos do Android e docs/icone.png gravados')
}
