# Como ajudar

O Vale a Corrida depende do que a Uber, a 99 e o inDrive mostram na tela, e essas telas mudam.
A ajuda que mais vale é dizer quando uma oferta deixou de ser reconhecida.

## Sem programar

1. No aplicativo, aba **Mais**, ligue **Guardar as últimas leituras**.
2. Espere a oferta aparecer de novo.
3. Volte à aba Mais e toque em **Compartilhar**.
4. Abra uma [issue](https://github.com/fabrinygabrielrc-eng/vale-a-corrida/issues/new/choose) e cole o texto.

Apague endereços e nomes antes de colar. O que interessa é o formato das linhas.

## Programando

O projeto tem duas partes:

- `nucleo/`: Kotlin puro, sem Android. Acha a oferta no texto (`LeitorDeOferta`) e faz as contas
  (`Avaliador`). É onde quase todo conserto acontece.
- `app/`: o aplicativo Android. O serviço de acessibilidade (`leitura/ServicoDeLeitura`) recolhe o
  texto da tela, por acessibilidade ou pela imagem, e mostra o cartão.

Para consertar uma oferta não reconhecida:

1. Copie as linhas da leitura para um teste novo em
   `nucleo/src/test/kotlin/app/valeacorrida/nucleo/LeitorDeOfertaTest.kt`.
2. Rode `./gradlew :nucleo:test` e veja o teste falhar.
3. Ajuste `LeitorDeOferta` até passar, sem quebrar os outros.

O leitor não deve depender do desenho de um aplicativo específico (posição na tela, nome de
componente): ele procura um valor em reais e os trechos de tempo e distância. É isso que o mantém
funcionando quando a tela muda.

### Comandos

```
./gradlew test                        # todos os testes
./gradlew :app:assembleDebug          # gera o aplicativo de teste
./gradlew :app:recordRoborazziDebug   # desenha as telas em app/build/telas, sem celular
```

Precisa do JDK 17 e do SDK do Android (o Android Studio traz os dois).

### Combinados

- Código e comentários em português.
- O aplicativo não pede permissão de internet. Mudança que precise dela não entra.
- O aplicativo não aceita nem recusa corridas, e não toca na tela pelo motorista.
- Toda mudança no leitor vem com teste.
