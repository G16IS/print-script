# Módulo `interpreter`

Dependencias: `common`. Tests también tiran de `lexer`, `parser` e `infrastructure` (integración end-to-end).

Ejecuta un `SyntaxProgram` ya parseado y produce efectos observables (`SideEffect`) tejiendo un `InterpreterContext` inmutable en el camino. No hace análisis sintáctico ni de tipos: asume que el programa viene del parser (y eventualmente validado por el type-checker).

**Está cableado** en `ExecuteCode` (CLI `run`). `interpretCode` sigue cortando en el type-checker. Los tests de integración del interpreter hacen lex+parse (sin type-check) y después `interpret`.

Mismo principio que lexer y parser: agregar una construcción nueva no toca el motor de dispatch, solo registra un `StatementExecutor`, `ExpressionEvaluator`, `BinaryOperationRule` o `CallHandler`. La tabla de operadores **sí** está hardcodeada (`DefaultTypeConfiguration`); no lee `type-system.config.json`.

---

## Cuándo tocarlo

- Nueva statement/expresión del lenguaje → executor/evaluator con `nodeNames` = nombres de regla del JSON + registro en la factory
- Nuevo call (`readInput`, etc.) → `CallHandler` + registro en la factory
- Nuevas combinaciones de tipos para operadores → regla en `DefaultTypeConfiguration`
- Política de errores de runtime o formato de output

---

## API pública

```kotlin
interface Interpreter {
    fun interpret(context: InterpreterContext, program: SyntaxProgram): Result<List<SideEffect>, RuntimeError>
}

object DefaultInterpreterFactory {
    fun create(
        typeConfiguration: TypeConfiguration = DefaultTypeConfiguration,
    ): DefaultInterpreter
}
```

`interpret` es la única entrada. `DefaultInterpreter` implementa `BlockExecutor`: el fold de statements queda ahí para que un `if` futuro reciba la interface sin depender de la clase concreta.

Errores: **Result end-to-end, sin excepciones**. `interpret`, `solve`, `evaluate` y `execute` devuelven `Result<_, RuntimeError>` (`map` / `flatMap` / `fold`). Fail-fast. Wiring incompleto o nodo malformado también es `Err` (`UnresolvableExpression` / `UnrecognizedNode`). Los constructores no validan ni lanzan. Handler duplicado: last-wins (`associateBy`).

---

## Mapa de archivos

```
interpreter/src/main/kotlin/printscript/
  Interpreter.kt                  interface: solo interpret
  DefaultInterpreter.kt           Interpreter + BlockExecutor: dispatch por node.name
  DefaultInterpreterFactory.kt    arma solver + executors
  InterpreterContext.kt           entorno inmutable copy-on-write
  RuntimeValue.kt                 NumberValue | StringValue | UnitValue
  ResultExt.kt                    zip interno (lookups puros, no solve)
  node/
    NodeAccess.kt                 tokenValue / childAt / firstChild / namedChild
    AstNames.kt                   nombres de regla/captura de grammar.config.json
  statement/
    BlockExecutor.kt              fun interface del fold de statements
    StatementExecutor.kt          nodeNames + execute(node, context, solver)
    StatementResult.kt            sideEffects + newContext
    VariableDeclarationExecutor.kt   let x: T = expr;  (object)
    ExpressionStatementExecutor.kt   <expr>;           (object)
  expression/
    ExpressionEvaluator.kt        nodeNames + evaluate(..., solver)
    ExpressionSolver.kt           interface solve(...)
    DefaultExpressionSolver.kt    dispatch por node.name
    EvalResult.kt                 value + sideEffects
    GroupEvaluator.kt             passthrough de ( expr )  (object)
    literal/
      NumberLiteralEvaluator.kt      toDouble finito o InvalidLiteral (object)
      StringLiteralEvaluator.kt      exige comillas (object)
      IdentifierEvaluator.kt         lookup o UndeclaredIdentifier (object)
    binaryoperation/
      BinaryOperationRule.kt      apply nullable
      TypeConfiguration.kt        interface
      DefaultTypeConfiguration.kt object: number + - * /, string+string
      BinaryOperationEvaluator.kt dispatch plano → binaryParts / evalOperands / apply
    call/
      CallHandler.kt              callee + handle(EvalResult)
      PrintlnHandler.kt           println → PrintEffect + UnitValue (object)
      CallEvaluator.kt            despacha handlers por nombre
```

---

## Dispatch

Un nivel: `node.name` es el nombre de regla de `grammar.config.json`. `DefaultInterpreter` y `DefaultExpressionSolver` indexan handlers por `nodeNames`. Nombre sin handler → `UnresolvableExpression`. Call desconocido → `UnresolvableCall`.

Un evaluator puede declarar más de un nombre (`BinaryOperationEvaluator` cubre `expression` y `term`, la misma forma de `LeftRule`). Nombre duplicado: last-wins.

Handlers default (`AstNames`):

| Regla | Handler |
|---|---|
| `variable` | `VariableDeclarationExecutor` |
| `expression-stmt` | `ExpressionStatementExecutor` |
| `expression`, `term` | `BinaryOperationEvaluator` |
| `number` | `NumberLiteralEvaluator` |
| `string` | `StringLiteralEvaluator` |
| `identifier` | `IdentifierEvaluator` |
| `call` | `CallEvaluator` |
| `group` | `GroupEvaluator` |

El interpreter no llama `SyntaxNode.value()` / `child()` / `find()`. Token y children van por `tokenValue()` / `childAt()` / `firstChild()` / `namedChild()` → `UnrecognizedNode` si faltan.

Los evaluators evitan pirámides: un `when` raso de dispatch y helpers con nombre (`binaryParts`, `evalOperands`, `calleeAndArgument`, `nameAndExpression`). `zip` solo combina lookups puros; `solve` de operandos es siempre secuencial (izquierda, después derecha) para no evaluar el derecho si el izquierdo falla y para preservar el orden de `println`.

---

## Modelo de ejecución

**Efectos.** `println` es una expresión (`factor → call`). Cada evaluación devuelve `EvalResult(value, sideEffects)`. `PrintlnHandler` agrega el `PrintEffect`; `println(println(1))` acumula en orden de evaluación.

**Valores.** `NumberValue(Double)` finito (`Infinity` / `NaN` → `InvalidLiteral`). `StringValue` sin comillas; el literal tiene que venir wrapped en `"..."`. `UnitValue` es el resultado de un call: como operando aritmético pega `InvalidOperands`.

**Ops en runtime vs type-checker.** `DefaultTypeConfiguration` tiene `+ - * /` entre numbers y `+` entre strings. **No** tiene `string + number`. El type-checker sí acepta `"a" + 1`.

**Formato de números al imprimir:** enteros sin decimales (`7`); decimales tal cual (`1.5`). `toPrintableString()`.

**Contexto.** `InterpreterContext` inmutable copy-on-write. `assignVariable` → `Result<InterpreterContext, RuntimeError>`. Los executors devuelven `StatementResult`; `DefaultInterpreter` teje el contexto.

**División por cero:** guard en `apply` → `DivisionByZero`.

**Tipos declarados:** el intérprete no chequea initializer vs anotación.

---

## Forma del árbol que consume

- `LeftRuleHandler` **siempre envuelve**: `expression` sin operador queda con un solo hijo (passthrough).
- El operador de un binario está en `children[1]`.
- En un `variable`, el id llega como hijo `"ID"` (`AstNames.ID`), no como `"identifier"`.
- Un call anidado es válido para el parser: `1 + println(x)` falla en runtime (`InvalidOperands`).

---

## Tests

| Archivo | Qué cubre |
|---|---|
| `InterpreterContextTest` | scopes, shadowing, assign con rebuild de cadena, assign no declarada |
| `ExpressionSolverTest` | literales (comillas, Infinity/NaN), identificador, binarios, unario, div-cero, operandos inválidos, calls anidados, callee desconocido, grupo vacío, errores de dispatch |
| `DefaultInterpreterTest` | threading de contexto vía `interpret` + `program(...)`, redeclaración, orden de efectos, fail-fast, last-wins, nombre sin handler |
| `InterpreterIntegrationTest` | `.ps` real lex→parse→interpret |

Helpers de test: `support/Results.kt` (`ok` / `err`), `support/Programs.kt` (`program(...)`).

Correr: `./gradlew :interpreter:test`.

Nota: los tests usan su propio `LanguageConfig` (`support/PsSupport`) con `partial` de número que soporta decimales. El `partial` de `language.config.json` corta `1.5` — gap del lexer.

---

## Cómo extender

### Nueva expresión (ej. comparaciones)

1. `ExpressionEvaluator` nuevo con `nodeNames` = el/los nombres de regla del JSON (`object` si no tiene deps).
2. Registrar en `DefaultInterpreterFactory.defaultEvaluators`.
3. Si necesita tabla de tipos: reglas en `DefaultTypeConfiguration`.
4. No hay enum ni mapping aparte: el dispatch es `node.name`.

### Nuevo call (ej. `readInput`)

1. `object FooHandler : CallHandler` con `callee` y `handle`.
2. Agregarlo a `CallEvaluator(listOf(PrintlnHandler, FooHandler))` en la factory.
3. No editar un `when` en `CallEvaluator`.

### Nuevo statement (ej. asignaciones sueltas, if)

1. `StatementExecutor` registrado en `defaultStatementExecutors`.
2. Si el cuerpo repite statements (bloques), pasar el `BlockExecutor` a `execute` — el fold vive en `DefaultInterpreter` detrás de esa interface.
