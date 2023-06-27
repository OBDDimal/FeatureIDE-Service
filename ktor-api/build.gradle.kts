val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgresql_version: String by project
val hikaricp_version: String by project

plugins {
    application
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

kotlin {
    jvmToolchain(19)
}


group = "de"
version = "0.0.1"
application {
    if (project.hasProperty("cli")) {
        if (project.property("cli")!!.equals("conf")) {
            mainClass.set("de.featureide.service.util.Configurator")
        } else if (project.property("cli")!!.equals("conv")) {
            mainClass.set("de.featureide.service.util.Converter")
        } else if (project.property("cli")!!.equals("slice")) {
            mainClass.set("de.featureide.service.util.Slicer")
        } else if (project.property("cli")!!.equals("prop")) {
            mainClass.set("de.featureide.service.util.Propagator")
        }
    } else {
        mainClass.set("io.ktor.server.netty.EngineMain")
    }

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("com.zaxxer:HikariCP:$hikaricp_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.xerial:sqlite-jdbc:3.30.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")

    implementation(files("lib/de.ovgu.featureide.lib.fm-v3.9.2.jar"))
    implementation(files("lib/org.sat4j.core.jar"))
    implementation(files("lib/antlr-3.4.jar"))
    implementation(files("lib/uvl-parser.jar"))
    implementation(files("lib/SPLCAT.jar"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.1")

    //kotlin commandline parser
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}
