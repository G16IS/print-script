# Módulo `interpreter`

Dependencias: `common`. Tests también tiran de `lexer`, `parser` e `infrastructure` (integración end-to-end).

Ejecuta un `SyntaxProgram` ya parseado y produce efectos observables (`SideEffect`) tejiendo un `InterpreterContext` inmutable en el camino. No hace análisis sintáctico ni de tipos: asume que el programa viene del parser (y eventualmente validado por el type-checker).

**No está cableado** en `application`: `interpretCode` corta en el type-checker y `application` no depende de este módulo. Los tests de integración del interpreter hacen lex+parse (sin type-check) y después `interpret`.

Mismo principio que lexer y parser: agregar una construcción nueva no toca el motor de dispatch, solo registra un `StatementExecutor`, `ExpressionEvaluator`, `BinaryOperationRule` o `CallHandler`. La tabla de operadores **sí** está hardcodeada (`DefaultTypeConfiguration`); no lee `type-system.config.json`.

---

## Cuándo tocarlo

- Nueva statement/expresión del lenguaje → executor/evaluator nuevo + entrada en `NodeKind`
- Nuevo call (`readInput`, etc.) → `CallHandler` + registro en la factory
- Nuevas combinaciones de tipos para operadores → regla en `DefaultTypeConfiguration`
- Política de errores de runtime o formato de output
- **No** para nombres de reglas: el mapping nombre→`NodeKind` es configuración (`PrintScriptMapping`)

---

## API pública

```kotlin
interface Interpreter {
    fun interpret(context: InterpreterContext, program: SyntaxProgram): Result<List<SideEffect>, RuntimeError>
}

object DefaultInterpreterFactory {
    fun create(
        mapping: Map<String, NodeKind> = PrintScriptMapping.mapping,
        typeConfiguration: TypeConfiguration = DefaultTypeConfiguration,
    ): DefaultInterpreter
}
```

`interpret` es la única entrada. El fold de statements vive en `BlockExecutor` (`DefaultBlockExecutor`); `DefaultInterpreter` solo delega.

Errores: **Result end-to-end, sin excepciones**. `interpret`, `solve`, `evaluate` y `execute` devuelven `Result<_, RuntimeError>` (`map` / `flatMap` / `fold`). Fail-fast. Wiring incompleto o nodo malformado también es `Err` (`UnresolvableExpression` / `UnrecognizedNode`). Los constructores no validan ni lanzan. Handler duplicado: last-wins (`associateBy`).

---

## Mapa de archivos

```
interpreter/src/main/kotlin/printscript/
  Interpreter.kt                  interface: solo interpret
  DefaultInterpreter.kt           delega en BlockExecutor
  DefaultInterpreterFactory.kt    arma resolver + solver + executors
  InterpreterContext.kt           entorno inmutable copy-on-write
  RuntimeValue.kt                 NumberValue | StringValue | UnitValue
  ResultExt.kt                    zip interno (lookups puros, no solve)
  node/
    NodeKind.kt                   vocabulario cerrado
    NodeKindResolver.kt           nombre de regla -> NodeKind (Result)
    NodeAccess.kt                 tokenValue / childAt / firstChild / namedChild
    AstNames.kt                   "ID", "expression"
    PrintScriptMapping.kt         mapping default grammar.config.json v1
  statement/
    BlockExecutor.kt              fun interface del fold de statements
    DefaultBlockExecutor.kt       dispatch + threading de StatementResult
    StatementExecutor.kt          kind + execute(node, context, solver)
    StatementResult.kt            sideEffects + newContext
    VariableDeclarationExecutor.kt   let x: T = expr;  (object)
    ExpressionStatementExecutor.kt   <expr>;           (object)
  expression/
    ExpressionEvaluator.kt        kind + evaluate(..., solver)
    ExpressionSolver.kt           interface solve(...)
    DefaultExpressionSolver.kt    dispatch por Map<NodeKind, Evaluator>
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

Dos niveles:

1. `NodeKindResolver` traduce `node.name` a `NodeKind`.
2. `DefaultBlockExecutor` y `DefaultExpressionSolver` despachan por `Map<NodeKind, _>`. Kind mapeado sin handler → `UnresolvableExpression`. Call desconocido → `UnresolvableCall`.

Mapping default (`PrintScriptMapping`):

| Regla (grammar.config.json) | NodeKind |
|---|---|
| `variable` | `VARIABLE_DECLARATION` |
| `expression-stmt` | `EXPRESSION_STMT` |
| `expression`, `term` | `BINARY_OP` |
| `number` | `NUMBER_LITERAL` |
| `string` | `STRING_LITERAL` |
| `identifier` | `IDENTIFIER` |
| `call` | `CALL` |
| `group` | `GROUP` |

El interpreter no llama `SyntaxNode.value()` / `child()` / `find()`. Token y children van por `tokenValue()` / `childAt()` / `firstChild()` / `namedChild()` → `UnrecognizedNode` si faltan.

Los evaluators evitan pirámides: un `when` raso de dispatch y helpers con nombre (`binaryParts`, `evalOperands`, `calleeAndArgument`, `nameAndExpression`). `zip` solo combina lookups puros; `solve` de operandos es siempre secuencial (izquierda, después derecha) para no evaluar el derecho si el izquierdo falla y para preservar el orden de `println`.

---

## Modelo de ejecución

**Efectos.** `println` es una expresión (`factor → call`). Cada evaluación devuelve `EvalResult(value, sideEffects)`. `PrintlnHandler` agrega el `PrintEffect`; `println(println(1))` acumula en orden de evaluación.

**Valores.** `NumberValue(Double)` finito (`Infinity` / `NaN` → `InvalidLiteral`). `StringValue` sin comillas; el literal tiene que venir wrapped en `"..."`. `UnitValue` es el resultado de un call: como operando aritmético pega `InvalidOperands`.

**Ops en runtime vs type-checker.** `DefaultTypeConfiguration` tiene `+ - * /` entre numbers y `+` entre strings. **No** tiene `string + number`. El type-checker sí acepta `"a" + 1`.

**Formato de números al imprimir:** enteros sin decimales (`7`); decimales tal cual (`1.5`). `toPrintableString()`.

**Contexto.** `InterpreterContext` inmutable copy-on-write. `assignVariable` → `Result<InterpreterContext, RuntimeError>`. Los executors devuelven `StatementResult`; `DefaultBlockExecutor` teje el contexto.

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
| `DefaultInterpreterTest` | threading de contexto vía `interpret` + `program(...)`, redeclaración, orden de efectos, fail-fast, last-wins, kind sin handler |
| `InterpreterIntegrationTest` | `.ps` real lex→parse→interpret |

Helpers de test: `support/Results.kt` (`ok` / `err`), `support/Programs.kt` (`program(...)`).

Correr: `./gradlew :interpreter:test`.

Nota: los tests usan su propio `LanguageConfig` (`support/PsSupport`) con `partial` de número que soporta decimales. El `partial` de `language.config.json` corta `1.5` — gap del lexer.

---

## Cómo extender

### Nueva expresión (ej. comparaciones)

1. Entrada en `NodeKind` + `PrintScriptMapping`.
2. `ExpressionEvaluator` nuevo (`object` si no tiene deps).
3. Registrar en `DefaultInterpreterFactory.defaultEvaluators`.
4. Si necesita tabla de tipos: reglas en `DefaultTypeConfiguration`.

### Nuevo call (ej. `readInput`)

1. `object FooHandler : CallHandler` con `callee` y `handle`.
2. Agregarlo a `CallEvaluator(listOf(PrintlnHandler, FooHandler))` en la factory.
3. No editar un `when` en `CallEvaluator`.

### Nuevo statement (ej. asignaciones sueltas, if)

1. `StatementExecutor` registrado en `defaultStatementExecutors`.
2. Si el cuerpo repite statements (bloques), pasar el `BlockExecutor` a `execute` — el fold ya vive en `DefaultBlockExecutor` y no depende de `DefaultInterpreter`.
