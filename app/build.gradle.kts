import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val releaseKeys = listOf(
    "RELEASE_KEYSTORE_PATH", "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD"
)
val releaseValues = releaseKeys.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseValues.values.all { !it.isNullOrBlank() }
require(releaseValues.values.none { !it.isNullOrBlank() } || hasReleaseSigning) {
    "Release signing requires all four RELEASE_* environment variables."
}

// Pinned OFL font is fetched at build time, never at runtime. Generated resources are cached locally.
abstract class PrepareVazirmatn : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val root = outputDirectory.get().asFile
        val base = "https://raw.githubusercontent.com/rastikerdar/vazirmatn/v33.003/"
        val files = mapOf(
            "fonts/ttf/Vazirmatn-Regular.ttf" to "font/vazirmatn_regular.ttf",
            "fonts/ttf/Vazirmatn-Bold.ttf" to "font/vazirmatn_bold.ttf",
            "OFL.txt" to "raw/vazirmatn_license.txt"
        )
        for ((source, target) in files) {
            val out = root.resolve(target)
            out.parentFile.mkdirs()
            val connection = URI(base + source).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            if (target.endsWith(".ttf")) {
                require(bytes.size > 10_000 && bytes.take(4) == listOf<Byte>(0, 1, 0, 0)) { "Invalid Vazirmatn font: $source" }
            } else require(bytes.toString(Charsets.UTF_8).contains("SIL OPEN FONT LICENSE", ignoreCase = true)) { "Invalid font license" }
            out.writeBytes(bytes)
        }
    }
}

android {
    namespace = "ir.memberexpiry.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "ir.memberexpiry.manager"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("production") {
                storeFile = file(releaseValues.getValue("RELEASE_KEYSTORE_PATH")!!)
                storePassword = releaseValues.getValue("RELEASE_STORE_PASSWORD")!!
                keyAlias = releaseValues.getValue("RELEASE_KEY_ALIAS")!!
                keyPassword = releaseValues.getValue("RELEASE_KEY_PASSWORD")!!
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("production")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        val prepare = tasks.register<PrepareVazirmatn>("prepare${variant.name.replaceFirstChar { it.uppercase() }}Vazirmatn")
        variant.sources.res?.addGeneratedSourceDirectory(prepare, PrepareVazirmatn::outputDirectory)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
}
