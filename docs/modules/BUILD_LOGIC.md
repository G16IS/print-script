# Módulo `build-logic`

Included build (`pluginManagement { includeBuild("build-logic") }`). No es un `include(...)` del pipeline y **no** depende de `common`. Los módulos Kotlin del repo no dependen de este como library: consumen el convention plugin `printscript.quality`.

Calidad de **este** código Kotlin (estilo + análisis estático). No es el linter/formatter de PrintScript (esos viven en [LINTER.md](LINTER.md) / [FORMATTER.md](FORMATTER.md)).

---

## Cuándo tocarlo

- Cambiar reglas o versiones de ktlint / detekt
- Agregar otra tarea de calidad al convention plugin
- Extraer el plugin a otro repo
- **No** para reglas de estilo del lenguaje PrintScript (módulos `linter` / `formatter`) ni para cambiar el parser/lexer

---

## API pública

Plugin id: `printscript.quality` (el archivo `printscript.quality.gradle.kts`).

Aplica, en el mismo classloader:

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

En el **root** (el plugin también se aplica ahí, sin ktlint/detekt):

| Tarea | Qué hace |
|---|---|
| `installGitHooks` | Si `core.hooksPath` no apunta a `hooks/`, lo configura y deja los scripts ejecutables |

```
./gradlew ktlintCheck          # format check, todos los módulos
./gradlew ktlintFormat         # reescribe fuentes — no en CI
./gradlew detekt               # lint estático, todos los módulos
./gradlew test                 # tests de todos los módulos
./gradlew installGitHooks      # git hooks del repo, no-op si ya están
```

CI (`.github/workflows/`, aparte de testing):

| Workflow | Archivo | Comando |
|---|---|---|
| Tests | `tests.yml` | `./gradlew test --continue` |
| Lint | `lint.yml` | `./gradlew detekt --continue` |
| Format | `format.yml` | `./gradlew ktlintCheck --continue` |

Hoy el plugin se aplica al **root** (solo `installGitHooks`) y a **todos** los subproyectos desde el `build.gradle.kts` raíz (`gradle.beforeProject`), sin editar cada `*/build.gradle.kts`. ktlint y detekt **fallan** si hay findings: el código existente de `parser` (y el resto) todavía no está limpio.

---

## Mapa de archivos

```
build-logic/
  settings.gradle.kts              pluginManagement + repos (self-contained)
  build.gradle.kts                 kotlin-dsl + classpath del plugin
  src/main/kotlin/
    printscript.quality.gradle.kts convention plugin
    printscript/InstallGitHooks.kt tarea installGitHooks
    printscript/PrintScriptExec.kt JavaExec de ps-run / ps-lint / …
```

En la raíz del repo (cableado, no es este módulo):

```
settings.gradle.kts                pluginManagement { includeBuild("build-logic") }
build.gradle.kts                   aplica printscript.quality a cada subproyecto
config/detekt/detekt.yml           override de reglas (buildUponDefaultConfig)
```

`detekt.yml` se resuelve con `project.rootDir` del proyecto que aplica el plugin (el monorepo), no el included build. Hoy el override es mínimo: `ForbiddenComment` sigue activo para `FIXME:` y `STOPSHIP:`; no lista `TODO:`.

---

## Classpath del plugin

`build-logic` es extraíble: versiones pinneadas acá, no en `gradle/libs.versions.toml`.

| Dependencia | Por qué |
|---|---|
| `org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10` | detekt 2 importa `KotlinBasePlugin`; ktlint necesita `KotlinProjectExtension` **en el mismo classloader** que el convention plugin |
| `org.jlleitschuh.gradle.ktlint` plugin marker `14.2.0` | `ktlintCheck` / `ktlintFormat` |
| `dev.detekt` plugin marker `2.0.0-alpha.6` | plugin id `dev.detekt` (no el `io.gitlab.arturbosch.detekt` de 1.x) |

Sin el KGP en `implementation`, `generatePrecompiledScriptPluginAccessors` explota (`NoClassDefFoundError: KotlinBasePlugin`) y, si compilara, ktlint no vería las clases de Kotlin del módulo.

detekt 2.x: `jvmTarget` es `Property<String>` (`.set("21")`). Los reportes archivo (html / checkstyle / sarif / markdown) van **apagados**: los findings salen por consola. ktlint: `outputToConsole` + `verbose`.

---

## Extraer a otro repo

1. Copiar la carpeta `build-logic/`.
2. En el `settings.gradle.kts` del otro repo:

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

3. Aplicar el plugin. O bien en el root (como acá):

```kotlin
plugins {
    id("printscript.quality")
}

gradle.beforeProject {
    if (this != rootProject) {
        pluginManager.apply("printscript.quality")
    }
}
```

o en un módulo:

```kotlin
plugins {
    id("printscript.quality")
}
```

No hace falta (y suele romper) aplicar `org.jetbrains.kotlin.jvm` **con versión** en el mismo proyecto: el convention plugin ya lo aplica. Si el módulo ya trae `alias(libs.plugins.kotlin.jvm)`, Gradle lo tolera si la versión coincide (hoy `2.4.10` en el catalog).
