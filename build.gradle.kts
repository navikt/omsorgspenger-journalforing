import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.13.1"
val junitPlatformVersion = "1.13.1"
val k9rapidVersion = "1.20250616113614-9760e08"
val dusseldorfKtorVersion = "7.0.0"
val ktorVersion = "3.1.0"
val jsonassertVersion = "1.5.3"
val orgJsonVersion = "20250517"
val mockkVersion = "1.14.2"
val openhtmltopdfVersion = "1.0.10"
val verapdfVersion = "1.28.1"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "2.1.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.sonarqube") version "6.2.0.5505"
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("org.json:json:$orgJsonVersion")

    // PDF
    implementation("com.openhtmltopdf:openhtmltopdf-core:$openhtmltopdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    testImplementation("org.verapdf:validation-model:$verapdfVersion")

    // Test
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("no.nav.k9.rapid:river-test:$k9rapidVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "x-access-token"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "21"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClass
                )
            )
        }
    }

    withType<Wrapper> {
        gradleVersion = "8.8"
    }

}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_omsorgspenger-journalforing")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
    }
}
