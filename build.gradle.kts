import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.7.0"
val k9rapidVersion = "1.0e6a99e"
val ktorVersion = "1.5.0"
val dusseldorfKtorVersion = "1.5.0.0d3bd1e"
val jsonassertVersion = "1.5.0"
val orgJsonVersion = "20200518"
val mockkVersion = "1.10.5"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("org.json:json:$orgJsonVersion")

    // Test
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
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
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    jcenter()
}

tasks {

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
        gradleVersion = "6.7.1"
    }

}
