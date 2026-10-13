# Instruções para agentes (Codex, Claude Code e outros)

Vale a Corrida é um aplicativo Android de código aberto para motoristas de aplicativo: lê a oferta
de corrida na tela da Uber, da 99 e do inDrive e mostra por cima quanto ela paga por km e por hora.
Leia o `README.md` antes de mexer.

## Mapa

- `nucleo/`: Kotlin puro, sem Android. `LeitorDeOferta` acha a oferta no texto; `Avaliador` faz as
  contas; `Linhas` junta os pedaços de texto em linhas. `Gastos` tem as categorias de gasto e o
  relatório do mês (`Relatorios`); `Datas` escreve as datas. Testes em `nucleo/src/test`.
- `app/`: o aplicativo.
  - `leitura/ServicoDeLeitura`: serviço de acessibilidade. Decide quando ler, por texto ou por imagem.
  - `leitura/LeitorDeTela`: texto da árvore de acessibilidade. `leitura/LeitorDeImagem`: texto da captura (ML Kit).
  - `leitura/Sobreposicao` e `leitura/CartaoView`: o cartão por cima da tela.
  - `ui/`: as telas, em Compose. `Preferencias`: o que a pessoa escolheu. `Diagnostico`: leituras guardadas.
  - `leitura/VezDaImagem`: uma leitura por imagem de cada vez, com prazo para a captura chegar.
  - `Rastro`: o caminho de cada leitura, sem o conteúdo da tela. Com "Guardar as últimas leituras"
    ligado, o serviço também faz uma ronda de 3 em 3 segundos, para o caso de o aplicativo de
    corrida não avisar as mudanças de tela.
  - `Registro`: a mesma história do rastro, escrita na hora no registro do Android, para quem está
    com o celular no cabo. Só fala depois de `adb shell setprop log.tag.ValeACorrida DEBUG`
    (e o serviço precisa ser ligado de novo depois disso); ler com `adb logcat -s ValeACorrida`.
  - `LivroDeGastos`: os gastos anotados, num arquivo do aparelho. `ui/TelaGastos`: anotar e ver o relatório.

## Comandos

```
./gradlew test                        # todos os testes (núcleo e aplicativo)
./gradlew :app:assembleDebug          # gera o APK de teste
./gradlew :app:recordRoborazziDebug   # desenha as telas em app/build/telas
```

Mudou tela ou cartão: rode o `recordRoborazziDebug` e **olhe as imagens** antes de dizer que ficou bom.

## Regras que não se quebram

1. **Sem permissão de internet.** O `AndroidManifest.xml` tira `INTERNET` e `ACCESS_NETWORK_STATE`
   de propósito. Nenhuma dependência que precise de rede.
2. **O aplicativo não aceita nem recusa corridas**, e não toca na tela pelo motorista. Só mostra a conta.
3. **Só lê os aplicativos de corrida que a pessoa ligou.** Nunca a tela de outro aplicativo.
4. **O leitor não depende do desenho de um aplicativo específico** (posição, nome de componente).
   Ele procura valor em reais e trechos de tempo e distância. É o que o mantém vivo quando a tela muda.
5. **Toda mudança no leitor vem com teste.** Oferta não reconhecida entra primeiro como teste em
   `LeitorDeOfertaTest`, com as linhas exatamente como vieram, e só depois o leitor é ajustado.
6. Código, comentários e textos em português. Comentário explica o porquê, não repete o código.

## Visual

- A rua de noite: asfalto, faixa e placas. As cores e a letra estão em `ui/Tema.kt`.
- **Verde, amarelo e vermelho são só do veredito da corrida.** Em nenhum outro lugar.
- Os blocos de uma tela não são caixas: ficam no fundo, separados pela faixa tracejada (`Faixa`).
- A ação principal e os avisos são placas: azul das placas de serviço, com filete branco por
  dentro (`BotaoPrincipal`, `PlacaDeAviso`). Cada tipo de gasto tem a sua placa (`PlacaDeServico`),
  com desenho em `ui/Pictogramas.kt`.
- A letra é a Overpass (`res/font`), feita a partir das placas de estrada. Licença em `docs/licencas`.
- O ícone sai de `ferramentas/icone/gerar.cjs`. Não edite os vetores à mão.

## O que foi conferido com corrida de verdade, e o que não foi

Uber, outubro de 2026, num Xiaomi com Android 15 (HyperOS 3), com o celular no cabo:

- **A Uber esconde a oferta deste serviço.** Com o cartão de oferta na tela, a janela da Uber
  entrega zero pedaços de texto (sem oferta, entrega os seis da tela do mapa). Uma ferramenta de
  sistema (`uiautomator dump`) enxerga o texto inteiro: é o Android 14 em diante escondendo a
  tela de serviços que não se declaram `isAccessibilityTool`. Não declaramos, porque o
  aplicativo não é uma ferramenta de acessibilidade. Logo, na Uber a leitura é **pela imagem**.
- **O Android às vezes não responde ao `takeScreenshot`.** No registro do sistema aparece
  `ScreenCaptureListenerWrapper consumer not alive` e nem `onSuccess` nem `onFailure` são
  chamados. Até a 0.2.1 isso deixava o serviço esperando para sempre: o cartão aparecia uma vez e
  nunca mais. `VezDaImagem` dá a captura por perdida depois do prazo e o serviço tenta de novo.
- **O formato real da oferta** (Envios Moto) está em `LeitorDeOfertaTest`, no teste "uber envios
  moto como veio do celular": um trecho só, o total, escrito "17 min (4.5 km)", com ponto e com o
  espaço que não quebra linha.
- A Uber avisa a mudança de tela quando a oferta entra: foi o aviso que puxou a primeira leitura
  nas três ofertas acompanhadas. Com a oferta parada na tela os avisos continuam, mais raros.

Não conferido: a 99 e o inDrive, e a Uber em outros aparelhos e versões do Android. Os formatos
desses testes foram escritos de memória. Não trate esses formatos como fato: quando chegar uma
leitura real, ela vale mais que o teste antigo.

Quando o cartão não aparecer para alguém, comece pelo caminho da leitura, não pelo leitor: com
cabo, o `Registro`; sem cabo, o texto do **rastro** (aba Mais > Compartilhar). Os dois dizem se o
serviço estava rodando, se o aplicativo avisa as mudanças de tela, quanto texto chegou e o que a
captura devolveu.

## Cuidados no Android

- A sobreposição usa `TYPE_ACCESSIBILITY_OVERLAY` com `FLAG_NOT_TOUCHABLE`: o cartão nunca pode
  receber toque, senão cobre o botão de aceitar.
- `takeScreenshot` só existe do Android 11 em diante e não pode ser chamado mais de três vezes por segundo.
  A resposta dele pode nunca chegar: nada no serviço pode ficar esperando por ela sem prazo.
- O simulador (`ui/TelaSimulador`) escreve os controles como "Minutos de viagem: 14", nunca "14 min":
  o leitor tomaria o controle por um trecho da corrida.
- Os gastos são gravados com troca única de arquivo (`LivroDeGastos.gravar`). Não troque por
  escrever direto no arquivo: celular que desliga no meio perderia o mês de alguém.
