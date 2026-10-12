# Instruções para agentes (Codex, Claude Code e outros)

Vale a Corrida é um aplicativo Android de código aberto para motoristas de aplicativo: lê a oferta
de corrida na tela da Uber, da 99 e do inDrive e mostra por cima quanto ela paga por km e por hora.
Leia o `README.md` antes de mexer.

## Mapa

- `nucleo/`: Kotlin puro, sem Android. `LeitorDeOferta` acha a oferta no texto; `Avaliador` faz as
  contas; `Linhas` junta os pedaços de texto em linhas. Testes em `nucleo/src/test`.
- `app/`: o aplicativo.
  - `leitura/ServicoDeLeitura`: serviço de acessibilidade. Decide quando ler, por texto ou por imagem.
  - `leitura/LeitorDeTela`: texto da árvore de acessibilidade. `leitura/LeitorDeImagem`: texto da captura (ML Kit).
  - `leitura/Sobreposicao` e `leitura/CartaoView`: o cartão por cima da tela.
  - `ui/`: as telas, em Compose. `Preferencias`: o que a pessoa escolheu. `Diagnostico`: leituras guardadas.

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

## O que ainda não foi conferido

Até a versão 0.1 ninguém rodou o aplicativo num celular, nem leu uma oferta de verdade da Uber ou
da 99. Os formatos nos testes foram escritos de memória. Não trate esses formatos como fato:
quando chegar uma leitura real, ela vale mais que o teste antigo.

## Cuidados no Android

- A sobreposição usa `TYPE_ACCESSIBILITY_OVERLAY` com `FLAG_NOT_TOUCHABLE`: o cartão nunca pode
  receber toque, senão cobre o botão de aceitar.
- `takeScreenshot` só existe do Android 11 em diante e não pode ser chamado mais de três vezes por segundo.
- O simulador (`ui/TelaSimulador`) escreve os controles como "Minutos de viagem: 14", nunca "14 min":
  o leitor tomaria o controle por um trecho da corrida.
