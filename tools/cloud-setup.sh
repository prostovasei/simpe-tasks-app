#!/usr/bin/env bash
# Настройка облачной среды Claude Code для сборки Simple Tasks (решение PM: переезд в новую сессию одной командой).
# Запуск из корня репозитория: bash tools/cloud-setup.sh. Повторный запуск ничего не ломает.
# Что делает: Android SDK (ставит, если нет), путь к нему (local.properties), зеркало Maven Central (центральный репозиторий отвечает 429 через прокси)
# для Gradle и Robolectric. Файлы ~/.gradle — вне репозитория, поэтому в новой среде их нужно создать заново.
set -euo pipefail

SDK=/opt/android-sdk
MIRROR=https://maven-central.storage-download.googleapis.com/maven2
# compileSdk 37 из app/build.gradle.kts; build-tools 36 нужны AGP 9.4.
CLT_BUILD=9862592
SDK_PACKAGES=("platform-tools" "platforms;android-37.0" "build-tools;37.0.0" "build-tools;36.0.0")
ROOT=$(cd "$(dirname "$0")/.." && pwd)

# Android SDK: в части облачных контейнеров его нет — ставим командной строкой Google (сеть должна пускать dl.google.com).
if [ ! -d "$SDK/platforms" ]; then
    echo "Нет Android SDK в $SDK — устанавливаю"
    TMP=$(mktemp -d)
    curl -sSfLo "$TMP/clt.zip" "https://dl.google.com/android/repository/commandlinetools-linux-${CLT_BUILD}_latest.zip"
    mkdir -p "$SDK/cmdline-tools"
    rm -rf "$SDK/cmdline-tools/latest"
    unzip -q -o "$TMP/clt.zip" -d "$TMP"
    mv "$TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
    rm -rf "$TMP"
    SM="$SDK/cmdline-tools/latest/bin/sdkmanager"
    yes | "$SM" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
    "$SM" --sdk_root="$SDK" "${SDK_PACKAGES[@]}" >/dev/null
fi
echo "sdk.dir=$SDK" > "$ROOT/local.properties"

mkdir -p ~/.gradle/init.d
cat > ~/.gradle/init.d/central-mirror.gradle.kts <<KTS
val mirror = "$MIRROR"

fun RepositoryHandler.redirectCentral() {
    all {
        if (this is MavenArtifactRepository && url.toString().trimEnd('/').let {
                it == "https://repo.maven.apache.org/maven2" || it == "https://repo1.maven.org/maven2"
            }) {
            setUrl(mirror)
        }
    }
}

beforeSettings {
    pluginManagement.repositories.redirectCentral()
    dependencyResolutionManagement.repositories.redirectCentral()
}
allprojects {
    buildscript.repositories.redirectCentral()
    repositories.redirectCentral()
}
KTS

touch ~/.gradle/gradle.properties
grep -q '^robolectricRepoUrl=' ~/.gradle/gradle.properties \
    && sed -i "s|^robolectricRepoUrl=.*|robolectricRepoUrl=$MIRROR|" ~/.gradle/gradle.properties \
    || echo "robolectricRepoUrl=$MIRROR" >> ~/.gradle/gradle.properties

echo "Готово: local.properties, зеркало Maven и robolectricRepoUrl настроены."
