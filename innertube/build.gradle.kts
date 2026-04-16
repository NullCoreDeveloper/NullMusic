plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation(libs.brotli)
    implementation(libs.extractor) {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
    testImplementation(libs.junit)
}
