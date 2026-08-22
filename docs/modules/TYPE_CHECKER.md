# Módulo `type-checker`

Chequeo de tipos estático y gestión de scopes. Pipeline objetivo:

```
lexer → parser → type-checker → interpreter
```

Hoy el código vive desconectado en Gradle `:semantic` (sobre el AST tipado leftover). Se migra a un módulo `type-checker` (o rename) que trabaja **exclusivamente** sobre `SyntaxProgram` / `SyntaxNode`.

No ejecuta. No formatea. No hace estilo (eso es linter). Solo valida tipos y declaraciones.

---

## Principios de diseño (innegociables)

1. **Inmutabilidad y pureza**  
   Todo estado se transforma devolviendo copias. No hay `mutableMap`, no hay side-effects en el núcleo. `Scope` y `ScopeStack` son data classes inmutables.

2. **Config-driven**  
   Tipos, literales, operaciones y la forma de los nodos a chequear viven en `type-system.config.json`. El código solo implementa un conjunto pequeño y estable de *kinds*.

3. **Separación de capas**  
   Grammar = sintaxis pura. Type-system = semántica de tipos. No se contaminan.

4. **SOLID**
    - **S**: `ExpressionTypeResolver` solo resuelve tipos de expresiones. `ScopeStack` solo gestiona símbolos. `TypeChecker` solo orquesta.
    - **O**: nuevos nodos se agregan mayormente por config; solo un *kind* nuevo requiere código.
    - **L/I/D**: handlers de *kind* detrás de interfaces; el orquestador depende de abstracciones.

5. **Errores con location**  
   Todo error carga la `Location` exacta del nodo/token responsable.

---

## Configuración (tres archivos + bundle)

Quedan **exactamente tres** archivos de configuración, bien separados:

| Archivo                     | Responsabilidad                              |
|-----------------------------|----------------------------------------------|
| `language.config.json`      | Tokens, matching, orden de prioridad         |
| `grammar.config.json`       | Producciones sintácticas                     |
| `type-system.config.json`   | Tipos, literales, operaciones, nodos semánticos |

### `type-system.config.json` — forma canónica

```json
{
  "types": ["number", "string"],

  "literals": {
    "NUMBER_LITERAL": "number",
    "STRING_LITERAL": "string"
  },

  "operations": [
    {
      "op": "+",
      "operands": ["number", "number"],
      "result": "number"
    },
    {
      "op": "+",
      "operands": ["string", "string"],
      "result": "string"
    },
    {
      "op": "+",
      "operands": ["string", "number"],
      "result": "string"
    },
    {
      "op": "-",
      "operands": ["number", "number"],
      "result": "number"
    },
    {
      "op": "*",
      "operands": ["number", "number"],
      "result": "number"
    },
    {
      "op": "/",
      "operands": ["number", "number"],
      "result": "number"
    }
  ],

  "nodes": {
    "variable": {
      "kind": "declaration",
      "id": "ID",
      "declaredType": "TYPE",
      "expression": "expression"
    },
    "expression-stmt": {
      "kind": "expression",
      "expression": "expression"
    },
    "expression": { "kind": "binary-or-primary" },
    "term":       { "kind": "binary-or-primary" },
    "factor":     { "kind": "primary" },
    "call": {
      "kind": "call",
      "callee": "CALL",
      "args": ["expression"]
    },
    "group": {
      "kind": "group",
      "expression": "expression"
    },
    "number":     { "kind": "literal" },
    "string":     { "kind": "literal" },
    "identifier": { "kind": "identifier" }
  }
}
```

**Notas sobre `operations`:**

- `operands` es una lista **ordenada**.
- El matcher prueba primero el orden exacto `(op, T1, T2)`.
- Si no encuentra y `T1 != T2`, prueba automáticamente la permutación `(op, T2, T1)` (comportamiento conmutativo por defecto).
- Si en el futuro aparece un operador no conmutativo se agregará `"commutative": false`.

**Notas sobre `nodes`:**

- Las claves son los **nombres de regla** que produce el parser (`SyntaxNode.name`).
- `kind` selecciona el handler que se ejecutará.
- Los demás campos indican **qué hijos** extraer (`child(name)`).
- Agregar un statement nuevo = agregar producción en grammar + entrada en `nodes`. Solo un *kind* completamente nuevo requiere código Kotlin.

### ConfigBundle y validación cruzada

`infrastructure` (o `application`) expone un cargador que:

1. Lee los tres JSON.
2. Valida cada uno internamente.
3. Ejecuta validaciones cruzadas:
    - Todo token referenciado en grammar existe en language.
    - Todo tipo que aparece en `operations` o `literals` existe en `types`.
    - Todo nombre de nodo referenciado en `nodes` existe como regla en grammar.
    - No hay referencias colgantes.
4. Devuelve un `LanguageDefinition` (o `ConfigBundle`) ya validado, o una lista de errores de configuración.

El type-checker **nunca** recibe configs crudas sin validar.

---

## API pública

```kotlin
// common/util/Result.kt  (genérico, reutilizable)
sealed interface Result<out T, out E> {
    data class Ok<T>(val value: T) : Result<T, Nothing>
    data class Err<E>(val error: E) : Result<Nothing, E>
}

data class Report<T, E>(
    val value: T? = null,
    val errors: List<E> = emptyList()
) {
    val isOk: Boolean get() = errors.isEmpty()
}
```

```kotlin
// type-checker
interface TypeChecker {
    /** Acumula todos los errores posibles (modo normal). */
    fun check(program: SyntaxProgram): Report<SyntaxProgram, TypeError>

    /** Falla al primer error (modo strict / tests). */
    fun checkStrict(program: SyntaxProgram): Result<SyntaxProgram, TypeError>
}
```

`TypeError` lleva siempre `location: Location` + mensaje claro en español.

---

## Componentes internos y responsabilidades (SOLID)

| Componente                | Responsabilidad única                                      | Inmutabilidad |
|---------------------------|------------------------------------------------------------|---------------|
| `TypeChecker`             | Orquesta el walk de statements, acumula errores            | puro          |
| `ExpressionTypeResolver`  | Dado un subárbol de expresión + Scope actual → tipo o error | puro          |
| `Scope`                   | Mapa inmutable nombre → tipo de **un** nivel               | data class    |
| `ScopeStack`              | Pila inmutable de `Scope`. push/pop/declare devuelven copia | data class    |
| `TypeSystemConfig`        | Modelo de la config ya validada                            | data class    |
| Handlers de *kind*        | Uno por kind (`DeclarationHandler`, `BinaryHandler`, …)    | estrategias   |

### ExpressionTypeResolver (pieza clave)

Recorre **solo** el subárbol de una expresión y calcula su tipo estáticamente:

```kotlin
interface ExpressionTypeResolver {
    fun resolve(
        expression: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig
    ): Result<String, TypeError>   // el tipo resultante o error
}
```

Algoritmo interno (recursivo, puro):

1. Según el `kind` del nodo (o su `name`):
    - **literal** → `config.literals[tokenType]`
    - **identifier** → `scope.lookup(name)` (error si no existe)
    - **binary-or-primary** →
        - si tiene OPERATOR: resolve(lhs) + resolve(rhs) → buscar en `operations` (con permutación)
        - si no: resolve del hijo primario
    - **group** → resolve del hijo expression
    - **call** → resolve de cada arg (el call en sí no tiene tipo de retorno por ahora)
2. Nunca ejecuta código. Solo combina tipos declarados en la config.
3. Como todo está tipado desde literales y la config, no existen “unknown values”.

El `TypeChecker` llama a este resolver cada vez que necesita el tipo de una expresión (declaraciones, statements de expresión, argumentos, etc.).

### Scope y ScopeStack (inmutables)

```kotlin
data class Scope(private val symbols: Map<String, String> = emptyMap()) {
    fun declare(name: String, type: String): Scope? =
        if (name in symbols) null else copy(symbols = symbols + (name to type))

    fun lookup(name: String): String? = symbols[name]
}

data class ScopeStack(private val scopes: List<Scope> = listOf(Scope())) {
    fun push(): ScopeStack = copy(scopes = scopes + Scope())

    fun pop(): ScopeStack =
        if (scopes.size <= 1) this else copy(scopes = scopes.dropLast(1))

    fun declare(name: String, type: String): ScopeStack? {
        val top = scopes.last().declare(name, type) ?: return null
        return copy(scopes = scopes.dropLast(1) + top)
    }

    fun lookup(name: String): String? =
        scopes.asReversed().firstNotNullOfOrNull { it.lookup(name) }
}
```

Aunque el lenguaje v1 es scope plano, el stack ya está listo. Cuando aparezcan bloques/`if` solo se hace `push()` al entrar y `pop()` al salir.

---

## Algoritmo del TypeChecker (alto nivel)

```
check(program):
  scope = ScopeStack()
  errors = mutableListOf()          // solo el acumulador es local/mutable; el resto puro

  for each statement in program.statements:
    nodeConfig = config.nodes[statement.name]
    if nodeConfig == null → error “nodo no reconocido” + continue

    according to nodeConfig.kind:
      declaration:
        id        = statement.child(nodeConfig.id).value()
        declared  = statement.child(nodeConfig.declaredType).value()
        exprNode  = statement.child(nodeConfig.expression)

        // 1. tipo declarado existe en config.types?
        // 2. resolve(exprNode) con ExpressionTypeResolver
        // 3. ¿declared == resolved?
        // 4. scope.declare(id, declared)  → si null, redeclaración
        // 5. actualizar scope con la nueva copia

      expression:
        resolve(statement.child(...))  // solo para validar, se descarta el tipo

      // otros kinds análogos

  return Report(program, errors)
```

Orden crítico en declaraciones: **primero** se resuelve el tipo de la expresión (con el scope **actual**), **después** se registra el identificador. Así `let x: number = x;` falla correctamente.

---

## Qué hace (checklist de checks)

Para `let x: string = true;`:

1. ¿`"string"` existe en `config.types`? → error de tipo desconocido.
2. `ExpressionTypeResolver` sobre la expresión → obtiene el tipo real.
3. ¿tipo declarado == tipo de la expresión? → error de mismatch.
4. ¿el identificador ya existía en el scope actual? → error de redeclaración.
5. Todo error lleva la `Location` del token/nodo culpable.

Además:

- Identificador usado sin declarar.
- Operandos de operadores no permitidos por la tabla de `operations`.
- Validación de la propia config al cargar (tipos referenciados existen, etc.).

---

## Qué no hace

- Ejecutar el programa.
- Validar aridad o nombre de `println` (más adelante puede vivir en un kind `call` más rico o en el linter).
- Inferencia de tipos (el lenguaje exige anotación).
- Conversiones implícitas number ↔ string (solo las que estén explícitas en `operations`).
- Estilo / convenciones (linter).
- Parsear o tokenizar.

---

## Migración desde `:semantic`

1. Nuevo módulo Gradle `type-checker` (depende solo de `common`).
2. Borrar o dejar de usar `ASTDefinition.kt` y el walk sobre `Program`.
3. Implementar sobre `SyntaxNode` + la config nueva.
4. Cablear en `application` **después** del parser y **antes** del interpreter.
5. Reescribir tests sobre `SyntaxProgram` real (o ejemplos `.ps`).

---

## Extensibilidad futura

- Nuevo tipo (`boolean`) → solo config.
- Nuevo operador → solo config.
- Nuevo statement (ej. `if`) → grammar + entrada en `nodes` + (si hace falta) un *kind* nuevo.
- Bloques anidados → `push`/`pop` en el handler correspondiente.
- Microservicios / lenguaje más grande → el diseño ya está preparado: la mayor parte del crecimiento es declarativa.
