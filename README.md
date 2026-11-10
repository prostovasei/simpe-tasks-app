# simpe-tasks-app
Minimalist Android task tracker with Jetpack Glance widget.

- Общие правила Claude Code и Codex: [AGENTS.md](AGENTS.md)
- Текущее состояние: [docs/HANDOFF.md](docs/HANDOFF.md)
- Задачи: [GitHub Issues](https://github.com/prostovasei/simpe-tasks-app/issues); [статус переноса старого бэклога](docs/BACKLOG-MIGRATION.md)
- Макеты: [design/README.md](design/README.md); проверки: [docs/TESTING.md](docs/TESTING.md)
- Требования: [`SPEC.md`](SPEC.md)
- Архитектура, аудит и план итераций: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## Как получить APK без Android Studio

1. GitHub → вкладка **Actions** → последний зелёный запуск **Android CI**.
2. Внизу страницы, раздел **Artifacts** → скачать `simple-tasks-debug-apk`.
3. Распаковать zip и установить APK на телефон (Android 10+, разрешить установку из неизвестных источников).

Там же лежит артефакт `screenshots` — изображения экранов в светлой и тёмной теме.
Все сборки подписаны одним debug-ключом из репозитория, поэтому новая версия ставится поверх старой без потери данных.

## Сборка локально

```bash
./gradlew assembleDebug           # APK: app/build/outputs/apk/debug/
./gradlew check                   # unit-тесты, lint, проверка дизайн-токенов
./gradlew recordRoborazziDebug    # скриншоты экранов: app/screenshots/
```

Нужны JDK 17+ и Android SDK (путь в `local.properties` → `sdk.dir` или переменная `ANDROID_HOME`).
