# Módulo `linter`

> **Estado: implementado.** Documenta la arquitectura, interfaces, reglas y configuración del analizador estático de código de PrintScript.

Dependencias Gradle: `common`. En tests incluye `lexer`, `parser` e `infrastructure` para pruebas de integración end-to-end.

Análisis **estático de estilo y convenciones** sobre el árbol sintáctico (`SyntaxProgram` / `SyntaxNode`). No chequea tipos (eso es `:type-checker`), no ejecuta (`:interpreter`), no reescribe fuentes (`:formatter`).

---

## Cuándo tocarlo

- Nueva regla de linting
- Cambiar cómo se recorre el árbol o cómo se acumulan errores de lint
- Extender formatos de nombres o tipos de `LintError`
- **No** para prender/apagar una regla ya existente: eso es `linter.config.json`
- **No** para reglas de trivia/whitespace (espacios alrededor de `:`, `=`): eso es el formatter, ya que `SyntaxNode` no conserva espacios inter-tokens

---

## Posición en el pipeline

```
lexer → parser → ┬→ type-checker
                 └→ linter
```

El linter es **puramente sintáctico**: no necesita tipos ni scopes. Corre sobre el mismo `SyntaxProgram` que produce el parser.

---

## API pública

```kotlin
// linter: printscript.Linter
interface Linter {
    fun lint(program: SyntaxProgram): Report<SyntaxProgram, LintError>
}

// factory: printscript.factory.DefaultLinterFactory
object DefaultLinterFactory {
    fun defaultProviders(): List<LintRuleProvider>
    fun create(
        config: LinterConfig,
        providers: List<LintRuleProvider> = defaultProviders(),
    ): Linter
}
```

El módulo **no lanza** ante violaciones encontradas en el código; las acumula en un `Report<SyntaxProgram, LintError>`. Sí lanza excepciones en tiempo de construcción (`DefaultLinterFactory.create`) si la configuración contiene IDs de reglas desconocidos, opciones inválidas o proveedores duplicados.

### Modelo de Errores (`common`)

Los errores de linting siguen la misma jerarquía sellada que `TypeError` y `RuntimeError`:

```kotlin
// common: printscript.error
sealed interface LintError {
    val message: String
    val location: Location
}

data class InvalidIdentifierFormat(
    val identifier: String,
    val expectedFormat: String,
    override val location: Location,
) : LintError {
    override val message: String
        get() = "El identificador '$identifier' no respeta el formato $expectedFormat"
}

data class InvalidPrintlnArgument(
    override val location: Location,
) : LintError {
    override val message: String
        get() = "La llamada a println solo acepta un identificador o un literal"
}
```

### Modelo de Reglas (`linter`)

```kotlin
// linter: printscript.rule
interface LintRule {
    val name: String
    fun supports(node: SyntaxNode): Boolean
    fun check(node: SyntaxNode): List<LintError>
}
```

- `supports(node)`: Determina si la regla aplica al nodo dado (por ejemplo, `node.name == "variable"` o `node.name == "call"`).
- `check(node)`: Evalúa el nodo y retorna una lista de `LintError` encontrados (vacía si cumple la regla).

---

## Mapa de archivos

```
linter/src/main/kotlin/printscript/
  Linter.kt                               interface pública
  DefaultLinter.kt                        recorrido con walker y acumulación en Report
  walk/
    SyntaxTreeWalker.kt                   DFS pre-order sobre SyntaxNode
  rule/
    LintRule.kt                           interface de regla
    IdentifierFormatRule.kt               regla identifier-format y enum LetterCase
    PrintlnArgumentRule.kt                regla println-simple-argument y unwrap de expresiones
  factory/
    LintRuleProvider.kt                   interface de provider (id + create)
    IdentifierFormatRuleProvider.kt       crea IdentifierFormatRule a partir de options
    PrintlnArgumentRuleProvider.kt        crea PrintlnArgumentRule a partir de options
    DefaultLinterFactory.kt               instancia Linter activando solo las reglas habilitadas en config

common/src/main/kotlin/printscript/
  error/LintError.kt                      jerarquía sellada de errores de lint
  domain/LinterConfig.kt                  modelo de configuración
  reader/LinterConfigReader.kt            interface lectora de configuración

infrastructure/src/main/
  kotlin/printscript/infrastructure/
    reader/JSONLinterConfigReader.kt      implementación de LinterConfigReader con kotlinx-serialization
    serializer/config/
      LinterConfigSerializer.kt           serializadores para LinterConfig y RuleConfig
  resources/
    linter.config.json                    archivo de configuración por defecto
```

---

## Recorrido del Árbol y Motor

`DefaultLinter.lint(program)`:
1. Itera cada statement de `program.statements`.
2. Para cada statement, invoca un método privado de lint que utiliza `SyntaxTreeWalker.walk(node)` (recorrido Depth-First Search pre-order).
3. Para cada nodo visitado, evalúa las reglas activas: si `rule.supports(node)`, acumula `rule.check(node)`.
4. Ordena las violaciones determinísticamente por `location.start.line` y `location.start.col`.
5. Retorna un `Report(program, errors)`.

---

## Configuración

### Modelo (`common`)

```kotlin
data class LinterConfig(
    val rules: Map<String, RuleConfig> = emptyMap(),
) {
    fun enabled(): Map<String, RuleConfig> = rules.filterValues { it.enabled }
}

data class RuleConfig(
    val enabled: Boolean,
    val options: Map<String, String> = emptyMap(),
)
```

### `linter.config.json` (`infrastructure`)

```json
{
  "rules": {
    "identifier-format": {
      "enabled": true,
      "options": {
        "format": "camelCase"
      }
    },
    "println-simple-argument": {
      "enabled": true,
      "options": {
        "callee": "println"
      }
    }
  }
}
```

### Providers y Factory

```kotlin
interface LintRuleProvider {
    val id: String
    fun create(options: Map<String, String>): LintRule
}
```

`DefaultLinterFactory.create(config, providers)`:
1. Valida que no existan providers duplicados con el mismo ID.
2. Filtra las reglas habilitadas (`config.enabled()`).
3. Busca el provider correspondiente a cada regla habilitada. Si el ID no existe, lanza `IllegalArgumentException`.
4. Cada provider valida sus opciones (e.g. formato `camelCase` o `snake_case`) y construye la instancia de `LintRule`.
5. Retorna `DefaultLinter(activeRules)`.

---

## Reglas Implementadas

### 1. `identifier-format`
- **Provider ID:** `"identifier-format"`
- **Opciones:** `"format"`: `"camelCase"` (default) o `"snake_case"`
- **`supports`:** `node.name == "variable"`
- **`check`:** Extrae el hijo `"ID"` y valida su nombre contra el patrón de `LetterCase`. Si no cumple, genera `InvalidIdentifierFormat` con la ubicación del token `"ID"`.

### 2. `println-simple-argument`
- **Provider ID:** `"println-simple-argument"`
- **Opciones:** `"callee"`: nombre de la función a restringir (default `"println"`)
- **`supports`:** `node.name == "call"` y `CALL` token es igual a `callee`
- **`check`:** Desenenvuelve la cadena de wrappers de expresiones mediante `unwrap` (desciende a través de nodos de un solo hijo como `expression -> term -> ...` y grupos entre paréntesis `group -> expression`). Verifica que el nodo resultante sea un argumento simple (`"identifier"`, `"number"`, `"string"`). Si contiene operaciones binarias o llamadas anidadas, genera `InvalidPrintlnArgument`.

---

## Invariantes

- El linter no lee archivos directamente; recibe `LinterConfig` armado por `infrastructure`.
- Una regla solo reporta sobre el nodo recibido.
- Toda instancia de `LintError` conserva una `Location` precisa con línea y columna de inicio y fin.
- Opciones de configuración inválidas fallan en la etapa de instanciación en la factoría (`DefaultLinterFactory`).
- La ejecución del linter acumula errores en un `Report` sin interrumpir la validación de los siguientes statements.

---

## Suite de Tests

| Test | Cobertura |
|---|---|
| `SyntaxTreeWalkerTest` | Recorrido pre-order DFS, árboles ramificados, cadenas de hijos únicos |
| `IdentifierFormatRuleTest` | Validación camelCase, snake_case, nodos no soportados, reporte de Location en ID |
| `PrintlnArgumentRuleTest` | Argumentos simples (identificadores, literales numéricos, cadenas), paréntesis (`group`), rechazo de binarios y llamadas |
| `DefaultLinterTest` | Programas vacíos, programas limpios, acumulación de múltiples violaciones, ordenamiento por Location |
| `DefaultLinterFactoryTest` | Filtrado de reglas activas/desactivadas, rechazo de IDs desconocidos, opciones inválidas y duplicados |
| `JSONLinterConfigReaderTest` | Deserialización JSON de configuración por defecto y filtrado de reglas |
| `LinterIntegrationTest` | Integración end-to-end con lexer y parser sobre código PrintScript real |
