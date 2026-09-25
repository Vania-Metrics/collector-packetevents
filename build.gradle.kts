// collector-packetevents — a vania-metrics-collector-packetevents-<v>.jar in build/libs/
//
// One module = one jar, loaded by the platform if — and only if — the core is
// present ("depend: [VaniaMetrics]" in plugin.yml). No third-party jar is
// bundled in it: everything below is compileOnly.
plugins {
    java
}

// The version is this collector's own, kept by release-please in version.txt; the core it
// compiles against is vaniaCore.ref, in gradle.properties.
val vaniaCoreDir = gradle.extra["vaniaCoreDir"] as File
version = file("version.txt").readText().trim()

// Velocity reads the version from @Plugin, which wants a compile-time constant: the build writes
// one from version.txt, so the proxy shows this collector's version and not the core's.
val generateBuildVersion by tasks.registering {
    val v = version.toString()
    val out = layout.buildDirectory.dir("generated/sources/buildVersion/java/main")
    inputs.property("version", v)
    outputs.dir(out)
    doLast {
        val file = out.get().file("fr/samflix/vaniametrics/module/packetevents/BuildVersion.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package fr.samflix.vaniametrics.module.packetevents;\n\n" +
                "/** This jar's version, written by the build from version.txt. */\n" +
                "public final class BuildVersion {\n\n" +
                "\tpublic static final String VALUE = \"$v\";\n\n" +
                "\tprivate BuildVersion() {}\n" +
                "}\n")
    }
}
sourceSets.main { java.srcDir(generateBuildVersion) }

dependencies {
    // Linked by the composite build to the core repo's api/ project.
    compileOnly("fr.samflix:vania-metrics-api")
    compileOnly(libs.bundles.paper)
    compileOnly(libs.velocity.api)
    compileOnly(libs.bungeecord.api)
    compileOnly(libs.packetevents)
    // @Plugin generates velocity-plugin.json: the processor is in velocity-api.
    annotationProcessor(libs.velocity.api)
}

tasks.withType<JavaCompile>().configureEach {
    // --release 21: the lobby targets Java 25, the proxy Java 21. Lowest wins.
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-path,-processing,-options", "-Werror"))
}

tasks.processResources {
    val v = version.toString()
    inputs.property("version", v)
    filesMatching(listOf("plugin.yml", "bungee.yml")) { filter { it.replace("\${version}", v) } }
}

tasks.jar {
    archiveFileName = "vania-metrics-${rootProject.name}-$version.jar"
    from("LICENSE") { into("META-INF") }
}

// Integration tests on real servers: see testkit/collector-it.gradle.kts in the core.
apply(from = vaniaCoreDir.resolve("testkit/collector-it.gradle.kts"))
