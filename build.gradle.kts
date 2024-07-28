val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"

}
group = "ru.l0sty"
version = "0.0.1"

application {
    mainClass.set("ru.l0sty.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")

    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-rate-limit:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:5.1.0")
    implementation("org.litote.kmongo:kmongo-id-serialization:5.1.0")

    implementation("io.github.crackthecodeabhi:kreds:0.9.1")

    implementation("io.viascom.nanoid:nanoid:1.0.1")
    implementation("dev.kord:kord-rest:0.14.0")
    implementation("io.github.crackthecodeabhi:kreds:0.9.1")


    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.16.1")
    // https://mvnrepository.com/artifact/commons-codec/commons-codec
    implementation("commons-codec:commons-codec:1.17.1")


}
