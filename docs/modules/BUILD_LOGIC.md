# Calidad del repo (`com.g16is.conventions` + `buildSrc`)

Los convention plugins de ktlint, detekt, kover y publishing viven en el repo hermano [`G16IS/gradle-conventions`](https://github.com/G16IS/gradle-conventions) y se consumen **solo** desde GitHub Packages:

`https://maven.pkg.github.com/G16IS/gradle-conventions`

Este repo **no** hace `includeBuild` de esa carpeta ni de `mavenLocal()`.

`PrintScriptExec` (tasks `ps-run` / `ps-lint` / …) queda en `buildSrc/` de este repo, package `printscript`. No viaja a gradle-conventions.

Calidad de **este** código Kotlin (estilo + análisis estático). No es el linter/formatter de PrintScript (esos viven en [LINTER.md](LINTER.md) / [FORMATTER.md](FORMATTER.md)).

---

## Cuándo tocarlo

- Cambiar reglas o versiones de ktlint / detekt / kover → repo `gradle-conventions`, publicar una versión nueva, bump del `version` en este root `build.gradle.kts`
- Cambiar las tasks CLI `ps-*` → `buildSrc/src/main/kotlin/printscript/PrintScriptExec.kt` y el root `build.gradle.kts`
- **No** para reglas de estilo del lenguaje PrintScript (módulos `linter` / `formatter`) ni para cambiar el parser/lexer

---

## API pública

Plugin ids (versión **1.0.0**):

| Id | Dónde se aplica |
|---|---|
| `com.g16is.conventions.quality` | root + cada subproyecto (`gradle.beforeProject`) |
| `com.g16is.conventions.coverage` | root + cada subproyecto |
| `com.g16is.conventions.publishing` | cada módulo que publica a Packages (`apply false` en el root) |

Quality aplica, en el mismo classloader, en **subproyectos**:

- `org.jetbrains.kotlin.jvm` (toolchain 21)
- `org.jlleitschuh.gradle.ktlint` — lint + format
- `dev.detekt` — análisis estático

Tareas que quedan en cada proyecto Kotlin:

| Tarea | Qué hace |
|---|---|
| `ktlintCheck` | Reporta estilo; no reescribe |
| `ktlintFormat` | Reescribe fuentes al estilo ktlint |
| `detekt` | Análisis estático; findings en consola (sin reportes archivo) |
| `test` | La que ya tenía el módulo |

En el **root** (quality sin ktlint/detekt):

| Tarea | Qué hace |
|---|---|
| `installGitHooks` | Copia `pre-commit` / `post-commit` del plugin a `hooks/` y setea `core.hooksPath` |

```
./gradlew ktlintCheck          # format check, todos los módulos
./gradlew ktlintFormat         # reescribe fuentes — no en CI
./gradlew detekt               # lint estático, todos los módulos
./gradlew test                 # tests de todos los módulos
./gradlew installGitHooks      # copia hooks del plugin y apunta core.hooksPath
```

CI (`.github/workflows/ci.yml`). Format/lint/wrapper en paralelo desde el arranque (no necesitan clases). `assemble` + `test` van en el mismo job para no recompilar en otra VM. Los jobs que corren `./gradlew` usan `GITHUB_TOKEN` + `packages: read` para resolver el plugin.

| Job | Comando |
|---|---|
| Gradle Wrapper | valida el checksum de `gradle-wrapper.jar` |
| Format (ktlint) | `./gradlew ktlintCheck --continue` |
| Lint (detekt) | `./gradlew detekt --continue` |
| Build + tests | `./gradlew assemble --continue` y después `./gradlew test --continue` |

ktlint y detekt **fallan** si hay findings.

Auth local para bajar el plugin (igual que el TCK): `gpr.user` / `gpr.key` en `gradle.properties`. Ese archivo está en `.gitignore`. CI usa `GITHUB_TOKEN` + `packages: read`. Publishing de los módulos PrintScript usa `gpr.owner=G16IS` y `gpr.repo=print-script`.

---

## Mapa de archivos

```
buildSrc/
  build.gradle.kts                 kotlin-dsl, toolchain 21
  src/main/kotlin/printscript/
    PrintScriptExec.kt             JavaExec de ps-run / ps-lint / …
```

En la raíz del repo:

```
settings.gradle.kts                pluginManagement → GitHub Packages conventions
build.gradle.kts                   aplica quality + coverage; registra ps-* y coverageReport
gradle.properties                  gpr.owner / gpr.repo (destino de publish de módulos)
config/detekt/detekt.yml           override de reglas (buildUponDefaultConfig)
hooks/                             generado por installGitHooks; gitignored
```

`detekt.yml` se resuelve con `project.rootDir` del proyecto que aplica el plugin. Si el archivo no existe, el plugin usa la config default de Detekt. Hoy el override es mínimo: `ForbiddenComment` / `ReturnCount` apagados.

---

## Classpath del plugin

Las versiones de ktlint / detekt / kover / KGP están pinneadas en `gradle-conventions`, no en `gradle/libs.versions.toml` de este repo.

| Dependencia (en gradle-conventions) | Por qué |
|---|---|
| `org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10` | detekt 2 importa `KotlinBasePlugin`; ktlint necesita `KotlinProjectExtension` **en el mismo classloader** que el convention plugin |
| `org.jlleitschuh.gradle.ktlint` plugin marker `14.2.0` | `ktlintCheck` / `ktlintFormat` |
| `dev.detekt` plugin marker `2.0.0-alpha.6` | plugin id `dev.detekt` (no el `io.gitlab.arturbosch.detekt` de 1.x) |
| `org.jetbrains.kotlinx:kover-gradle-plugin:0.9.9` | coverage |

detekt 2.x: `jvmTarget` es `Property<String>` (`.set("21")`). Los reportes archivo (html / checkstyle / sarif / markdown) van **apagados**: los findings salen por consola. ktlint: `outputToConsole` + `verbose`.

No hace falta (y suele romper) aplicar `org.jetbrains.kotlin.jvm` **con versión** en el mismo proyecto: el convention plugin ya lo aplica. Si el módulo ya trae `alias(libs.plugins.kotlin.jvm)`, Gradle lo tolera si la versión coincide (hoy `2.4.10` en el catalog).
