# Módulo `type-checker`

Dependencias Gradle: `common`. Lo usa `application` (después del parser).

Chequeo de tipos estático y gestión de scopes sobre `SyntaxProgram` / `SyntaxNode`. Pipeline:

```
lexer → parser → type-checker → interpreter
```

Hoy está **implementado y cableado** en `interpretCode`. No ejecuta. No formatea. No hace estilo (eso es linter). Solo valida tipos y declaraciones.

El type-checker **no lee JSON ni paths**. Recibe un `TypeSystemConfig` ya construido (igual que el parser recibe `Grammar`). La lectura vive en `infrastructure`.

---

## Cuándo tocarlo

- Nuevo *kind* de nodo (hace falta un handler Kotlin)
- Cambiar cómo se resuelven tipos de expresiones o cómo se acumulan errores
- Scope / redeclaración / mismatch
- **No** para agregar un tipo o un operador que ya entre en la tabla: eso es [TYPE_SYSTEM_CONFIG.md](../configs/TYPE_SYSTEM_CONFIG.md)
- **No** para parsear, ejecutar ni formatear

---

## API pública

```kotlin
interface TypeChecker {
    fun check(program: SyntaxProgram): Report<SyntaxProgram, TypeError>
    fun checkStrict(program: SyntaxProgram): Result<SyntaxProgram, TypeError>
}

object DefaultTypeCheckerFactory {
    fun create(config: TypeSystemConfig): TypeChecker
}
```

Siempre crear el checker por la factory. La impl es `DefaultTypeChecker(config, DefaultExpressionTypeResolver())`.

- `check` recorre todos los statements y **acumula** errores. El `Report` lleva el programa y la lista.
- `checkStrict` corta en el **primer** error (`Result.Err`) o devuelve `Result.Ok(program)`.
- El módulo no lanza. `application` traduce un `Report` no-ok a `error("El chequeo de tipos falló:…")`.

`TypeError` (en este módulo) es `data class TypeError(message: String, location: Location)` con mensajes en español.

En `common` hay además un sealed `printscript.error.TypeError` con variantes (`TypeMismatch`, `UndeclaredIdentifier`, …). El pipeline **no** lo usa todavía: el checker y `interpretCode` consumen `printscript.typechecker.TypeError`.

---

## Mapa de archivos

```
type-checker/src/main/kotlin/printscript/typechecker/
  TypeChecker.kt                   interface
  DefaultTypeChecker.kt            walk de statements + registry de NodeHandler
  DefaultTypeCheckerFactory.kt     create(config)
  ExpressionTypeResolver.kt        interface + Default (registry de ExpressionKindHandler)
  Scope.kt / ScopeStack.kt         data classes inmutables
  TypeError.kt                     message + location
  handlers/
    NodeHandler.kt                 kind de statement → StatementCheck (scope + errors)
    DeclarationHandler.kt          kind "declaration"
    ExpressionStmtHandler.kt       kind "expression"
    ExpressionKindHandler.kt       kind de expresión → Result<String, TypeError>
    LiteralHandler.kt              "literal"
    IdentifierHandler.kt           "identifier"
    BinaryOrPrimaryHandler.kt      "binary-or-primary" + match de operations
    GroupHandler.kt                "group"
    CallHandler.kt                 "call" (valida args; el call no tiene tipo de retorno)
    PrimaryHandler.kt              "primary"
```

`TypeSystemConfig`, `Operation`, `NodeConfig` y `TypeSystemConfigReader` viven en `common`. El JSON y los serializers, en `infrastructure`.

---

## Configuración

Tres archivos, bien separados:

| Archivo                     | Responsabilidad                              |
|-----------------------------|----------------------------------------------|
| `language.config.v1.0.json`      | Tokens, matching, orden de prioridad         |
| `grammar.config.v1.0.json`       | Producciones sintácticas                     |
| `type-system.config.v1.0.json`   | Tipos, literales, operaciones, nodos semánticos |

Spec de ese JSON: [TYPE_SYSTEM_CONFIG.md](../configs/TYPE_SYSTEM_CONFIG.md). El archivo está en `../../infrastructure/src/main/resources/type-system.config.v1.0.json`. El modelo `TypeSystemConfig` valida al construirse: todo tipo citado en `literals` u `operations` existe en `types`. No hay `ConfigBundle` cruzado con grammar/language todavía.

### `operations`

- `operands` es una lista **ordenada**.
- El matcher prueba primero `(op, T1, T2)`.
- Si no encuentra y `T1 != T2`, prueba la permutación `(op, T2, T1)` cuando `commutative` (default `true`).
- `"a" + 1` pega `string, number`. `1 + "a"` entra por permutación.

### `nodes`

- Las claves son `SyntaxNode.name` (reglas del parser).
- `kind` elige el handler.
- Los demás campos dicen **qué hijos** extraer (`child(name)`): `id`, `declaredType`, `expression`, `callee`, `args`.
- El código **no** hardcodea `"variable"` ni `"expression"`. Un statement nuevo = grammar + entrada en `nodes`. Solo un *kind* nuevo requiere Kotlin.

---

## Componentes (SOLID)

| Componente                | Responsabilidad única                                       |
|---------------------------|-------------------------------------------------------------|
| `TypeChecker`             | Orquesta el walk de statements, acumula errores             |
| `ExpressionTypeResolver`  | Subárbol de expresión + scope → tipo o error                |
| `Scope` / `ScopeStack`    | Símbolos inmutables; `declare`/`push`/`pop` devuelven copia |
| `TypeSystemConfig`        | Config ya validada (en `common`)                            |
| Handlers de *kind*        | Uno por kind, detrás de interfaz                            |

`DefaultExpressionTypeResolver` y `DefaultTypeChecker` despachan por `handlers[nodeConfig.kind]`. Kind desconocido → error con location.

Orden en declaraciones: **primero** se resuelve el initializer (scope actual), **después** se registra el id. `let x: number = x;` falla.

El único acumulador mutable es la lista local de errores en `check`.

---

## Qué hace

- Tipo declarado existe en `config.types`
- Tipo del initializer compatible con la anotación
- Redeclaración en el mismo scope
- Identificador no declarado
- Operandos según la tabla de `operations` (`+` number+number, string+string, string+number)
- Args de `call` (el call no tiene tipo de retorno)
- Todo error lleva `Location`

## Qué no hace

- Ejecutar el programa
- Validar aridad o nombre de `println`
- Inferencia de tipos (el lenguaje exige anotación)
- Conversiones implícitas fuera de `operations`
- Estilo (linter) / parsear / tokenizar

---

## Tests

- Unitarios: `Scope`/`ScopeStack`, resolver (literales, ids, `1 + 2 * 3`, permutación, group, call), `TypeChecker` (match, mismatch, redeclaración, tipo inexistente, no declarado, varios errores vs `checkStrict`)
- Integración: `application` con `.ps` reales (los válidos y `type_mismatch`, `undeclared_variable`, `redeclaration`)

Árboles de unit test: fabricados a mano con la misma forma que el parser (`wrap` / binario de 3 hijos). No bajan al AST tipado viejo.

---

## Extensibilidad

- Nuevo tipo (`boolean`) o operador → [TYPE_SYSTEM_CONFIG.md](../configs/TYPE_SYSTEM_CONFIG.md) (y token/grammar si hace falta)
- Nuevo statement → grammar + entrada en `nodes` + (si el kind no existe) un handler
- Bloques anidados → `push`/`pop` en el handler correspondiente
