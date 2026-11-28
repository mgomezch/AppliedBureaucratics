plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
}

// The mod's textures are drawn by tools/textures at build time, not kept as images.
// See Note [Textures are drawn at build time] in tools/textures.
val textures by sourceSets.creating {
    java.srcDir("tools/textures/src")
}

val generateTextures by tasks.registering(JavaExec::class) {
    description = "Draws the mod's textures into build/generated/textures."
    group = "build"
    val font = file("tools/textures/fonts/NotoSerifDisplay-Regular.ttf")
    val out = layout.buildDirectory.dir("generated/textures")
    classpath = textures.runtimeClasspath
    mainClass.set("com.gtnewhorizons.appliedbureaucratics.textures.TextureGenerator")
    jvmArgs("-Djava.awt.headless=true")
    inputs.file(font)
    outputs.dir(out)
    argumentProviders.add(CommandLineArgumentProvider { listOf(font.absolutePath, out.get().asFile.absolutePath) })
}

sourceSets.main {
    resources.srcDir(generateTextures)
}
