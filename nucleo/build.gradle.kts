// O núcleo não depende do Android: lê o texto da oferta e faz as contas.
// Por isso os testes dele rodam em qualquer computador, sem celular nem emulador.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
