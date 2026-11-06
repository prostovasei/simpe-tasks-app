#!/usr/bin/env bash
# Настройка облачной среды Claude Code для сборки Simple Tasks (решение PM: переезд в новую сессию одной командой).
# Запуск из корня репозитория: bash tools/cloud-setup.sh. Повторный запуск ничего не ломает.
# Что делает: путь к Android SDK (local.properties), зеркало Maven Central (центральный репозиторий отвечает 429 через прокси)
# для Gradle и Robolectric. Файлы ~/.gradle — вне репозитория, поэтому в новой среде их нужно создать заново.
set -euo pipefail

SDK=/opt/android-sdk
MIRROR=https://maven-central.storage-download.googleapis.com/maven2
ROOT=$(cd "$(dirname "$0")/.." && pwd)

[ -d "$SDK" ] || { echo "Нет Android SDK в $SDK — сборка невозможна" >&2; exit 1; }
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
