# Módulo `linter`

> **Estado: diseño, no implementado.** Todo el código de este documento es **ilustrativo** (propuesta), salvo los tipos que se citan de `common` (`SyntaxNode`, `Location`, `Report`, `Result`), que ya existen.

Dependencias Gradle propuestas: `common`. Tests también tiran de `lexer`, `parser` e `infrastructure` para integración end-to-end (mismo esquema que `:interpreter`).

Análisis **estático de estilo y convenciones** sobre el árbol del parser. No chequea tipos (eso es `:type-checker`), no ejecuta (`:interpreter`), no reescribe fuentes (`:formatter`).

---

## Cuándo tocarlo

- Nueva regla de linting
- Cambiar cómo se recorre el árbol o cómo se acumulan violaciones
- Forma de `LintViolation` / política de severidad
- **No** para prender/apagar una regla ya existente: eso es `linter.config.json`
- **No** para reglas que necesiten whitespace del fuente (espacios alrededor de `:`, `=`): eso es el formatter, y el `SyntaxNode` no lo conserva

---

## Posición en el pipeline

```
lexer → parser → ┬→ type-checker
                 └→ linter
```

El linter es **puramente sintáctico**: no necesita tipos ni scopes. Corre sobre el mismo `SyntaxProgram` que produce el parser, en paralelo con el type-checker. No se cablea dentro de `interpretCode`; va en un caso de uso aparte (`analyzeCode`), que es la operación `Analyzing` del CLI de la consigna.

---

## API pública propuesta

```kotlin
// linter
interface Linter {
    fun lint(node: SyntaxNode): List<LintViolation>
    fun lint(program: SyntaxProgram): Report<SyntaxProgram, LintViolation>
}

object DefaultLinterFactory {
    fun create(
        config: LinterConfig,
        providers: List<LintRuleProvider> = PrintScriptRules.defaults(),
    ): Linter
}
```

Dos puertas a propósito:

- `lint(node)` — un statement. Es la que permite el modo **streaming** que pide la consigna: `parseNextStatement` → `lint` → descartar, sin acumular el fuente entero en memoria.
- `lint(program)` — conveniencia para tests e integración; internamente itera statements.

El módulo **no lanza** ante violaciones (son findings, no errores del linter). Sí lanza en construcción si la config es inválida. Mismo criterio que `TypeChecker`: `application` traduce el `Report` a salida de usuario.

### Modelo

```kotlin
data class LintViolation(
    val ruleId: String,      // "identifier-format"
    val message: String,     // en español, como TypeError
    val location: Location,  // start/end — la consigna exige fila y columna de inicio y fin
)

interface LintRule {
    val id: String
    fun supports(node: SyntaxNode): Boolean
    fun check(node: SyntaxNode): List<LintViolation>
}
```

`supports` + `check` es el mismo patrón que `RuleHandler.supports(rule)` en el parser. `check` devuelve lista (no `Result`) porque una regla puede producir 0..n violaciones sobre el mismo nodo y porque el linter **acumula**, no corta: `Report`, no `Result`.

---

## Mapa de archivos propuesto

```
linter/src/main/kotlin/printscript/linter/
  Linter.kt                     interface
  DefaultLinter.kt              walk + acumulación
  DefaultLinterFactory.kt       config + providers → reglas habilitadas
  LintViolation.kt
  LintRule.kt
  LintRuleProvider.kt           id + create(options)
  walk/
    SyntaxTreeWalker.kt         DFS pre-order sobre SyntaxNode
  rules/
    PrintScriptRules.kt         defaults(): providers de v1
    IdentifierFormatRule.kt     "identifier-format"
    PrintlnArgumentRule.kt      "println-simple-argument"
    NamingStyle.kt              camelCase | snake_case → regex
  node/
    PrintScriptNodeNames.kt     nombres de regla del grammar v1, en un solo lugar

common/src/main/kotlin/printscript/
  domain/LinterConfig.kt        modelo (sin JSON)
  reader/LinterConfigReader.kt  puerto

infrastructure/src/main/
  kotlin/printscript/infrastructure/
    reader/JSONLinterConfigReader.kt
    serializer/config/LinterConfigSerializer.kt
  resources/linter.config.json
```

El reparto sigue la invariante del repo: **el modelo de config vive en `common`, el JSON y los serializers en `infrastructure`, y el linter recibe el `LinterConfig` ya armado** — igual que el parser recibe `Grammar` y el type-checker recibe `TypeSystemConfig`. El linter nunca abre un archivo.

---

## Recorrido

`DefaultLinter.lint(node)`:

1. `SyntaxTreeWalker` hace DFS **pre-order** desde el nodo.
2. Para cada nodo visitado, recorre las reglas activas; si `supports(node)`, acumula `check(node)`.
3. Devuelve la lista acumulada.

`lint(program)` hace lo mismo por statement y arma `Report(program, violations)`.

**Regla de oro para evitar reportes duplicados:** una regla reporta **solo sobre el nodo que recibió**, aunque para decidir mire su subárbol. Si `PrintlnArgumentRule` matchea en `call` e inspecciona descendientes, el walker igual va a visitar esos descendientes después — pero ninguna otra regla los va a reportar por la misma causa.

`println(println(x))`: el `call` externo viola la regla (su argumento es un call, no un identificador ni un literal); el `call` interno se visita aparte y su argumento `x` sí es simple. Una sola violación. Correcto.

**Orden de salida:** ordenar por `location.start` (línea, después columna) antes de devolver. El pre-order ya sale casi en orden de fuente, pero una regla que matchea en `variable` reporta en la location del nodo padre y puede desordenarse respecto de hijos de otro statement. Ordenar explícitamente hace los tests determinísticos.

**Optimización posible (no v1):** en vez de preguntarle `supports` a cada regla por cada nodo (O(nodos × reglas)), indexar `Map<String, List<LintRule>>` por `SyntaxNode.name`. Requiere que la regla declare nombres en vez de un predicado. No hace falta con dos reglas; anotado por si crecen.

---

## Configuración

### Modelo (`common`)

```kotlin
data class LinterConfig(val rules: Map<String, RuleConfig>) {
    fun enabled(): Map<String, RuleConfig> = rules.filterValues { it.enabled }
}

data class RuleConfig(
    val enabled: Boolean,
    val options: Map<String, String> = emptyMap(),
)
```

### `linter.config.json`

```json
{
  "rules": {
    "identifier-format": {
      "enabled": true,
      "options": { "format": "camelCase" }
    },
    "println-simple-argument": {
      "enabled": true,
      "options": { "callee": "println" }
    }
  }
}
```

JSON y no YAML: la consigna acepta cualquiera de los dos y `infrastructure` ya tiene kotlinx-serialization montado. YAML sumaría una dependencia sin ganar nada.

### De config a reglas

```kotlin
interface LintRuleProvider {
    val id: String
    fun create(options: Map<String, String>): LintRule
}
```

`DefaultLinterFactory.create(config, providers)`:

1. Indexa providers por `id`; ids duplicados → `IllegalArgumentException` (mismo criterio que el dispatch del interpreter).
2. Para cada entrada **habilitada** de la config, busca el provider. Id desconocido → falla en construcción, no en silencio.
3. `provider.create(options)`; cada provider parsea y valida sus propias opciones. `"format": "PascalCase"` explota acá, no a mitad del lint.

**Decisión:** una regla ausente de la config queda **apagada**. La config es la única fuente de verdad sobre qué corre; nada de defaults implícitos que aparezcan solos cuando alguien agrega una regla nueva.

`options` es `Map<String, String>` a propósito: mantiene el linter y el modelo de `common` libres de JSON, al precio de ser stringly-typed en el borde. Cada provider convierte y valida.

---

## Reglas v1

### `identifier-format`

| | |
|---|---|
| Opciones | `format`: `camelCase` \| `snake_case` |
| `supports` | `node.name == "variable"` |
| `check` | `node.childOrNull("ID")?.value()` contra el regex del formato |
| Location | la del hijo `ID`, no la del `variable` |

Regexes propuestos: `^[a-z][a-zA-Z0-9]*$` (camelCase), `^[a-z][a-z0-9]*(_[a-z0-9]+)*$` (snake_case).

**Ojo con `ID` vs `identifier`.** PARSER.md lo avisa: en un `variable` el id se captura como `{ "capture": "ID" }` y el hijo se llama `"ID"`; en un uso es un `AtomRule` llamado `"identifier"` sobre el token `ID`. La regla se aplica en la **declaración**, no en los usos: así cada nombre malo se reporta una sola vez, en el lugar donde se puede arreglar.

### `println-simple-argument`

| | |
|---|---|
| Opciones | `callee`: nombre de la función a restringir (default `println`) |
| `supports` | `node.name == "call"` y el hijo `CALL` tiene `value() == callee` |
| `check` | desenvolver el argumento y verificar que sea `identifier`, `number` o `string` |

**Esta es la regla con trampa.** Por `LeftRuleHandler` que **siempre envuelve** (ver INTERPRETER.md y PARSER.md), `println(x)` produce:

```
call
  CALL("println")
  expression
    term
      identifier → ID("x")
```

O sea que "el argumento es un nodo `expression`" es **siempre** verdadero, tanto para `println(x)` como para `println(1 + 2)`. La regla no puede mirar el hijo directo: tiene que descender mientras el nodo tenga **exactamente un hijo** (passthrough) y recién ahí mirar qué quedó.

```kotlin
private fun unwrap(node: SyntaxNode): SyntaxNode =
    if (node.children.size == 1) unwrap(node.children.first()) else node
```

Si lo desenvuelto tiene `name` en `{ "identifier", "number", "string" }` → cumple. Si tiene 3 hijos (binario), o es un `call`, o un `group` con una expresión adentro → viola.

Un `group` (`println((x))`) tiene un solo hijo y se desenvuelve solo; si eso cuenta como "expresión" o no es una decisión de producto — la propuesta es que **sí cumpla**, porque semánticamente sigue siendo un identificador.

---

## Acoplamiento con la gramática

Las reglas hardcodean nombres de regla del `grammar.config.json` (`"variable"`, `"call"`, `"ID"`, `"identifier"`). Esto rompe la invariante del type-checker, que no hardcodea `"variable"` porque los nombres viven en `type-system.config.json`.

Es una desviación **consciente** y sigue el precedente de `DefaultTypeConfiguration` del interpreter, que también hardcodea su tabla. La razón: una regla de linting no es genérica sobre "un nodo cualquiera", tiene semántica propia; hacerla data-driven implicaría un mini-lenguaje de reglas en JSON que la consigna no pide.

Mitigación: los nombres van a **un solo archivo** (`PrintScriptNodeNames`), no dispersos por las reglas. Renombrar una producción en la gramática es un cambio de un archivo.

Alternativa si la coupling molesta: mover `NodeKind` de `:interpreter` a `:common` (ya estaba anotado como candidato para reuso desde el type-checker estático) y que el linter despache por *kind* en vez de por nombre de regla. Es una decisión de repo, no del linter — no la tomes solo por esto.

---

## Qué hace / qué no hace

**Hace:** formato de identificadores, restricción del argumento de `println`, acumulación de violaciones con `Location` exacta.

**No hace:**

- Tipos, scopes, declaraciones (`:type-checker`)
- Ejecutar (`:interpreter`)
- Reescribir fuentes (`:formatter`)
- Reglas de whitespace (espacios alrededor de `:` o `=`, saltos de línea). El `SyntaxNode` **no conserva trivia**: solo tiene locations, no los espacios entre tokens. Todas las reglas de whitespace de la consigna son del formatter, que trabaja sobre otra representación
- Errores de sintaxis: para cuando el linter ve el árbol, el parser ya falló o pasó

---

## Invariantes

- El linter no lee archivos ni JSON. Recibe `LinterConfig` armado.
- Una regla reporta solo sobre el nodo que recibió, aunque mire su subárbol.
- Toda `LintViolation` lleva `Location`. Sin excepciones: es requisito explícito de la consigna.
- Config inválida (id desconocido, opción inválida) falla **en construcción**, no durante el lint.
- El linter acumula y devuelve; no lanza por violaciones.
- Los nombres de regla de gramática viven en `PrintScriptNodeNames`, no esparcidos por las reglas.

---

## Cómo extender

### Regla nueva

1. `LintRule` con `id`, `supports`, `check`.
2. `LintRuleProvider` que parsea sus opciones y valida.
3. Registrar en `PrintScriptRules.defaults()`.
4. Entrada en `linter.config.json` (si no está en la config, no corre).
5. Test unitario con árbol fabricado a mano + caso en el test de integración.

No hace falta tocar `DefaultLinter` ni el walker. Mismo principio que lexer, parser e interpreter: agregar una construcción no toca el motor de dispatch.

### Nuevo formato de identificador

Solo `NamingStyle`. La regla no cambia.

---

## Tests propuestos

| Nivel | Qué cubre |
|---|---|
| `SyntaxTreeWalkerTest` | orden pre-order, nodo hoja, árbol vacío |
| `IdentifierFormatRuleTest` | camel ok/mal, snake ok/mal, location apunta al `ID` |
| `PrintlnArgumentRuleTest` | identificador ok, literal ok, binario mal, call anidado mal, group ok, callee distinto no aplica |
| `DefaultLinterTest` | acumulación de varias violaciones, regla apagada no corre, orden por location |
| `DefaultLinterFactoryTest` | id desconocido falla, provider duplicado falla, opción inválida falla |
| `LinterIntegrationTest` | `.ps` real lex→parse→lint con asserts sobre `List<LintViolation>` |

Árboles de unit test fabricados a mano con la **misma forma que produce el parser** (con el wrap de `LeftRule`), no aplanados — si no, `PrintlnArgumentRule` pasa los tests y falla en producción. Ese es exactamente el error que el harness tiene que evitar.

Para integración, ejemplos chicos que fijen **una** cosa cada uno: `bad_identifier.ps`, `println_expression.ps`, `clean.ps`.

---

## Decisiones abiertas

| Tema | Opciones | Nota |
|---|---|---|
| Severidad (`error` / `warning`) | agregar campo a `LintViolation` vs no tenerlo | La consigna no la pide. Sugerencia: no en v1 |
| `SyntaxTreeWalker` en `linter` o en `common` | El formatter probablemente lo va a necesitar | Invariante del repo: si lo necesitan dos módulos, va a `common`. Moverlo cuando pase, no antes |
| Naming en asignaciones (`x = 5`) | — | El `grammar.config.json` v1 **no tiene asignaciones**, solo `variable` y `expression-stmt`. La consigna sí las pide. Hasta que exista la producción, la regla no tiene qué lintear |
| Regla ausente de la config | apagada (propuesto) vs default del provider | Propuesto: apagada, config como única fuente de verdad |
| `NodeKind` a `common` | desacopla linter e interpreter de nombres de regla | Decisión de repo, no del linter |
