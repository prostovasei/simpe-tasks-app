plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.prostovasei.simpletasks"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.prostovasei.simpletasks"
        minSdk = 29
        targetSdk = 37
        // Номер сборки = число коммитов: растёт одинаково в CI и при локальной сборке,
        // поэтому любой новый APK ставится поверх старого как обновление.
        versionCode = providers.exec { commandLine("git", "rev-list", "--count", "HEAD"); isIgnoreExitValue = true }
            .standardOutput.asText.get().trim().toIntOrNull() ?: 1
        // 0.<итерация>.<доработка>: 0.5.0 — Итерация 05.
        versionName = "0.14.0"
    }

    signingConfigs {
        // Общий debug-ключ в репозитории: APK из CI и локальной сборки ставятся поверх друг друга без удаления данных.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Сжимать код внутри APK: файл в 2–3 раза меньше, его можно прислать в чат (лимит 30 МБ).
        dex { useLegacyPackaging = true }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            // Robolectric для Android 16+ обращается к внутренним классам JDK.
            test.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            // Необязательное зеркало Maven для Robolectric (задаётся в ~/.gradle/gradle.properties).
            providers.gradleProperty("robolectricRepoUrl").orNull?.let {
                test.systemProperty("robolectric.dependency.repo.url", it)
            }
            // Путь к реальной резервной копии TickTick для локальной проверки (в репозиторий файл не попадает).
            providers.gradleProperty("ticktickCsv").orNull?.let {
                test.systemProperty("ticktick.csv", it)
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

roborazzi {
    // Скриншоты экранов: ./gradlew recordRoborazziDebug
    outputDir.set(file("screenshots"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lib.recur)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.glance.appwidget.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/**
 * Изоляция дизайн-токенов (SPEC.md §3): HEX-цвета, готовые цвета Compose и dp-литералы
 * разрешены только в пакете designsystem/tokens. Нарушение останавливает сборку.
 */
abstract class CheckDesignTokensTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val rules = listOf(
            Regex("""Color\(\s*0x""") to "HEX-цвет",
            Regex(""""#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?"""") to "HEX-цвет в строке",
            Regex("""\bColor\.(Red|Green|Blue|Black|White|Gray|LightGray|DarkGray|Yellow|Cyan|Magenta)\b""") to "готовый цвет Compose",
            Regex("""\b\d+(\.\d+)?f?\.dp\b""") to "dp-литерал",
        )
        val violations = sources.files
            .filter { it.extension == "kt" && !it.invariantSeparatorsPath.contains("/designsystem/tokens/") }
            .sortedBy { it.path }
            .flatMap { file ->
                file.readLines().withIndex().flatMap { (index, line) ->
                    rules.filter { (regex, _) -> regex.containsMatchIn(line) }
                        .map { (_, name) -> "${file.path}:${index + 1}: $name — используйте токены AppTheme" }
                }
            }
        report.get().asFile.writeText(violations.joinToString("\n"))
        if (violations.isNotEmpty()) {
            throw GradleException("Нарушена изоляция дизайн-токенов:\n" + violations.joinToString("\n"))
        }
    }
}

val checkDesignTokens = tasks.register<CheckDesignTokensTask>("checkDesignTokens") {
    sources.from(fileTree("src/main/java"), fileTree("src/main/kotlin"))
    report.set(layout.buildDirectory.file("reports/design-tokens.txt"))
}

tasks.named("preBuild") { dependsOn(checkDesignTokens) }
