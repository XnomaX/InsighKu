import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

ksp {
    // Export Room schema so future migrations have a versioned baseline.
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Local config (gitignored) — e.g. the receipt-scan API base URL.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
fun prop(key: String): String = (localProperties.getProperty(key) ?: "").trim()

android {
    namespace = "com.example.insightku"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.insightku"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Receipt-scan (Donut/Flask) API. Online-only — no offline fallback.
        // Read from local.properties so the URL is never committed.
        buildConfigField(
            "String",
            "RECEIPT_API_BASE_URL",
            "\"${prop("receiptApiBaseUrl").trimEnd('/')}\""
        )
        buildConfigField("String", "RECEIPT_API_KEY", "\"${prop("receiptApiKey")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Dynamic runtime locale switching (LocaleHelper) — all languages must be
    // present in every install, so disable per-language APK splitting.
    bundle {
        language {
            enableSplit = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    lint {
        // LocaleFolder wants res/values-id renamed to values-in, citing the
        // legacy java.util.Locale rewriting of Indonesian "id" -> "in". That
        // rewriting does not apply at this target SDK: Android 15 removed the
        // conversion for apps targeting API 35+, and this app targets 36.
        //
        // MEASURED on the built debug APK: res/values-in is a symlink to
        // res/values-id, `aapt2 dump configurations` lists both `id` and `in`,
        // and a sampled string resolves identically under each qualifier.
        //
        // INFERRED FROM DOCUMENTATION, NOT VERIFIED ON A DEVICE (no emulator or
        // handset was attached): that carrying both qualifiers is what keeps
        // Indonesian resolving across API 24-36 — "id" on API 35+ and "in" on
        // API <= 34, where the runtime is documented to still use the legacy
        // code. Per-qualifier runtime resolution was not exercised.
        //
        // Suppressed rather than acted on: the suggested rename would discard
        // the modern code that targetSdk 36 requires.
        disable += "LocaleFolder"

        // The dependency versions below are a deliberately pinned known-good
        // baseline (see .omp/AGENTS.md "Known-good facts"). These two checks
        // only report that a newer release exists — GradleDependency for
        // AndroidX/Google artifacts, NewerVersionAvailable for everything else
        // (it also queries Maven Central on every run, making lint slow).
        // Upgrading is a separate, evidence-backed decision, not a lint fix,
        // so the noise is silenced instead of chased.
        //
        // Deliberately NOT disabled: OldTargetApi and AndroidGradlePluginVersion
        // stay visible because raising compileSdk/targetSdk or the AGP/Gradle
        // version changes runtime behaviour and needs its own review.
        disable += "GradleDependency"
        disable += "NewerVersionAvailable"
    }
}

/**
 * Indonesian resource-qualifier backport.
 *
 * Android reports the legacy language code "in" for Indonesian on API <= 34
 * (`LocaleHelper` documents this), so the APK must ship BOTH the `values-id`
 * (modern, API 35+) and `values-in` (legacy) qualifiers or Indonesian users
 * silently fall back to English. `locales_config.xml` deliberately lists only
 * "id", so the `values-in` folder is the sole carrier of the legacy qualifier.
 *
 * It used to be a git symlink (`values-in -> values-id`). A fresh Windows clone
 * materialises symlinks as a 9-byte text file unless Developer Mode/admin is
 * enabled, which makes AGP see a *file* named `values-in`, drops the qualifier
 * with no build error, and breaks Indonesian for whoever clones. Generate a real
 * copy into the build directory instead: OS-independent, and never duplicated in
 * source control.
 */
abstract class GenerateValuesIn : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    /** Res *root* — the parent of the `values-in` qualifier folder this task writes. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val qualifierDir = File(outputDir.get().asFile, "values-in")
        qualifierDir.deleteRecursively()
        sourceDir.get().asFile.copyRecursively(qualifierDir, overwrite = true)
    }
}

val generateValuesIn = tasks.register<GenerateValuesIn>("generateValuesIn") {
    sourceDir.set(layout.projectDirectory.dir("src/main/res/values-id"))
    outputDir.set(layout.buildDirectory.dir("generated/locale-res"))
}

// Register the res ROOT (not the qualifier folder) through the Variant API — AGP
// rejects a plain Provider on the legacy SourceSet API. `addGeneratedSourceDirectory`
// wires this task into resource merging itself, so no extra `dependsOn` is needed.
androidComponents.onVariants { variant ->
    variant.sources.res?.addGeneratedSourceDirectory(generateValuesIn) { it.outputDir }
}

kotlin {
    compilerOptions {
        // KT-73255: Hilt/Dagger @ApplicationContext on constructor props should hit param+field
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)

    // Testing
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose (BOM-managed)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // WorkManager + Hilt-Work
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Networking — receipt-scan API (Donut/Flask, online-only)
    implementation(libs.okhttp)

    // Lottie
    implementation(libs.lottie.compose)

    // Vico Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Firebase (BOM-managed)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
}
