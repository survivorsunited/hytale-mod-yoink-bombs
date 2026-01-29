# Contributing

Thanks for your interest in contributing to Yoink Bombs. This doc covers local setup, build/test workflows, and developer notes.

---

## Prerequisites

- Java 25 JDK
- Git
- A Hytale install (for server assets)

---

## Local Setup

### 1) Configure `.env`

Create or update `.env` in the repo root:

```txt
HYTALE_HOME=Z:\data\games\Hytale\Data
JDK_URL=https://api.adoptium.net/v3/binary/version/jdk-25.0.1+8/windows/x64/jdk/hotspot/normal/adoptium
```

`HYTALE_HOME` must point to the folder that contains `install` and `UserData`.

### 2) Download JDK 25 into `.jdk`

```ps1
.\downloadJdk.ps1
```

---

## Build

### Windows

```ps1
.\gradlew.bat compileJava
.\gradlew.bat shadowJar
.\gradlew.bat clean shadowJar
```

### Linux/Mac

```bash
./gradlew compileJava
./gradlew shadowJar
./gradlew clean shadowJar
```

The output jar is `build\libs\YoinkBombs-0.0.2.jar`.

---

## Test (Automated)

### Windows

```ps1
.\gradlew.bat runServer
```

### Linux/Mac

```bash
./gradlew runServer
```

This will:

- Download the Hytale server (cached for future runs)
- Build the plugin
- Copy it to the server's mods folder
- Load any extra test mods from `tests/mods` (if present)
- Start the server with interactive console

If a local server package zip exists at `C:\Users\<you>\.hytale\server\`, the latest zip is used instead of downloading.

To validate setup without starting the server:

```ps1
.\gradlew.bat prepareServer
```

The server PID is written to `run\server.pid`. Stop it with:

```ps1
.\gradlew.bat stopServer
```

---

## Debugging

### Windows

```ps1
.\gradlew.bat runServer -Pdebug
```

### Linux/Mac

```bash
./gradlew runServer -Pdebug
```

Then attach your IDE debugger to `localhost:5005`.

---

## Commands

- `yoinkbombs` lists available bomb variants.
- `yoinkbombs <variant> <attr> <number>` updates config values (OP-only).
- Grant `yoinkbombs.admin` to allow config updates.
- Grant `yoinkbombs.trusted` to allow players to use Yoink Bombs.

Current `attr` list:

- `BlockDamageRadius`

---

## Protection Integration

- WorldProtect is detected via reflection at runtime.
- If present, `RegionService.canBuild(...)` is used to skip protected blocks.

---

## Project Structure

```text
hytale-mod-yoink-bombs/
├── src/main/java/
├── src/main/resources/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Additional Notes (from template README)

Source: https://github.com/survivorsunited/realBritakee-hytale-template-plugin/blob/main/README.md

### CI/CD and Releases

- GitHub Actions builds on push, runs tests, and uploads artifacts.
- Tagging a release (e.g. `v1.0.0`) triggers a release build.

### Troubleshooting

- Clean rebuild: `./gradlew clean build --refresh-dependencies`
- If server won't start, verify `runHytale.jarUrl`, `runHytale.assetsPath`, and Java 25.
- If plugin won't load, confirm `manifest.json` main class and bundled deps.
- Server console may require `/auth login` for authenticated mode.

### Best Practices

- Use structured logging.
- Avoid blocking the main thread with heavy work.
- Handle errors gracefully.
