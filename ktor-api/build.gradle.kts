import de.undercouch.gradle.tasks.download.Download
import org.gradle.jvm.tasks.Jar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgresql_version: String by project
val hikaricp_version: String by project

val featureide_version: String = "v3.11.1"

val featureide_jar_url: String = "https://github.com/FeatureIDE/FeatureIDE/releases/download/${featureide_version}/de.ovgu.featureide.lib.fm-${featureide_version}.jar"
val antlr_jar_url: String = "https://github.com/FeatureIDE/FeatureIDE/raw/${featureide_version}/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar"
val sat4j_jar_url: String = "https://github.com/FeatureIDE/FeatureIDE/raw/${featureide_version}/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar"
val uvl_jar_url: String = "https://github.com/FeatureIDE/FeatureIDE/raw/${featureide_version}/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar"
val splcat_jar_url: String = "https://github.com/FeatureIDE/FeatureIDE/raw/${featureide_version}/plugins/de.ovgu.featureide.fm.core/lib/SPLCAT.jar"


plugins {
    application
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
    id("de.undercouch.download") version "5.4.0"
}


kotlin {
    jvmToolchain(17)
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
        } else if (project.property("cli")!!.equals("stat")) {
            mainClass.set("de.featureide.service.util.FeatureStats")
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
    implementation("io.ktor:ktor-server-cors:$ktor_version")
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

    implementation(files("lib/de.ovgu.featureide.lib.fm-$featureide_version.jar"))
    implementation(files("lib/org.sat4j.core.jar"))
    implementation(files("lib/antlr-3.4.jar"))
    implementation(files("lib/uvl-parser.jar"))
    implementation(files("lib/SPLCAT.jar"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.1")

    //kotlin commandline parser
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

tasks.register<Download>("download_jar_dependencies") {
    doFirst{
        src(listOf(
            featureide_jar_url,
            antlr_jar_url,
            sat4j_jar_url,
            uvl_jar_url,
            splcat_jar_url
        ))
        dest("lib")
        overwrite(false)
    }
}


val fatJar = task("fatJar", type = Jar::class) {
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "de.featureide.service.Application"
    }
    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)
}

defaultTasks("download_jar_dependencies")

tasks.compileKotlin {
    dependsOn("download_jar_dependencies")
}

tasks {
	"build" {
		dependsOn(fatJar)
	}
}
