# Módulo `interpreter`

Dependencias: `common`. Tests también tiran de `lexer`, `parser` e `infrastructure` (integración end-to-end).

Ejecuta un `SyntaxProgram` ya parseado y produce efectos observables (`SideEffect`) tejiendo un `InterpreterContext` inmutable en el camino. No hace análisis sintáctico ni de tipos: asume que el programa viene del parser (y eventualmente validado por el type-checker).

**Está cableado** en `ExecuteCode` (CLI `run`). `interpretCode` sigue cortando en el type-checker. Los tests de integración del interpreter hacen lex+parse (sin type-check) y después `interpret`.

Mismo principio que lexer y parser: agregar una construcción nueva no toca el motor de dispatch, solo registra un `StatementExecutor`, `ExpressionEvaluator` o `BinaryOperationRule` nuevo. La tabla de operadores **sí** está hardcodeada (`DefaultTypeConfiguration`); no lee `type-system.config.json`.

---

## Cuándo tocarlo

- Nueva statement/expresión del lenguaje → executor/evaluator nuevo + entrada en `NodeKind`
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
        typeConfiguration: TypeConfiguration = DefaultTypeConfiguration(),
    ): DefaultInterpreter
}
```

Errores: **Result end-to-end**. `interpret`, `solve`, `evaluate` y `execute` devuelven `Result<_, RuntimeError>` usando los helpers de `common/util/Result.kt` (`map` / `flatMap` / `fold`). Fail-fast: el primer error corta la ejecución y sube. `RuntimeError` vive en `common`; algunas variantes también son `TypeError` (`UndeclaredIdentifier`, `InvalidOperands`, `UnrecognizedNode`).

---

## Mapa de archivos

```
interpreter/src/main/kotlin/printscript/
  Interpreter.kt                  interface + Result
  DefaultInterpreter.kt           dispatch statements, threading de contexto
  DefaultInterpreterFactory.kt    arma resolver + solver + executors
  InterpreterContext.kt           entorno inmutable copy-on-write con parent chain
  RuntimeValue.kt                 NumberValue | StringValue | UnitValue (+ toPrintableString)
  node/
    NodeKind.kt                   vocabulario cerrado del intérprete
    NodeKindResolver.kt           nombre de regla -> NodeKind (Result)
    PrintScriptMapping.kt         mapping default grammar.config.json v1
  statement/
    StatementExecutor.kt          kind + execute(node, context, solver): Result<StatementResult, _>
    StatementResult.kt            sideEffects + newContext
    VariableDeclarationExecutor.kt   let x: T = expr;
    ExpressionStatementExecutor.kt   <expr>; (descarta valor, conserva efectos)
  expression/
    ExpressionEvaluator.kt        kind + evaluate(...) : Result<EvalResult, _>
    EvalResult.kt                 value + sideEffects (los efectos viajan acá)
    ExpressionSolver.kt           dispatch por Map<NodeKind, Evaluator>
    GroupEvaluator.kt             passthrough de ( expr )
    literal/
      NumberLiteralEvaluator.kt      toDouble()
      StringLiteralEvaluator.kt      strip de comillas
      IdentifierEvaluator.kt         lookup o UndeclaredIdentifier
    binaryoperation/
      BinaryOperationRule.kt      operator + left/right/result types + apply
      TypeConfiguration.kt        interface
      DefaultTypeConfiguration.kt reglas v1: number + - * /, string+string (no string+number)
      BinaryOperationEvaluator.kt unario passthrough, div-by-zero, InvalidOperands
    call/
      CallEvaluator.kt            println(x) -> PrintEffect + UnitValue
```

---

## Dispatch

Dos niveles, mismo patrón:

1. `NodeKindResolver` traduce `node.name` (nombre de regla de la gramática) a `NodeKind` vía un `Map<String, NodeKind>`.
2. `DefaultInterpreter` y `ExpressionSolver` despachan por `Map<NodeKind, _>`; duplicados explotan en construcción.

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

`DefaultInterpreter` valida en construcción que todo kind mapeado esté cubierto por un executor **o** un evaluator.

---

## Modelo de ejecución

**Efectos.** En esta gramática `println` es una expresión (`factor → call`), no un statement. Por eso cada evaluación de expresión devuelve `EvalResult(value, sideEffects)` y los evaluators combinan los efectos de sus hijos (literales e identificadores aportan lista vacía). `CallEvaluator` agrega el `PrintEffect`; `println(println(1))` acumula en orden de evaluación.

**Valores.** `NumberValue(Double)`, `StringValue(String)` — sin comillas, las saca el evaluator. `UnitValue` es el resultado de un call: si termina como operando de una operación aritmética, `TypeConfiguration` no tiene regla y falla con `InvalidOperands`.

**Ops en runtime vs type-checker.** `DefaultTypeConfiguration` tiene `+ - * /` entre numbers y `+` entre strings. **No** tiene `string + number`. El type-checker, leyendo `type-system.config.json`, sí acepta `"a" + 1` (y la permutación `1 + "a"` si `commutative`). Si cableás el interpreter después del checker, ese programa pasa tipos y pega `InvalidOperands` al ejecutar.

**Formato de números al imprimir:** enteros sin decimales (`7`, no `7.0`); decimales tal cual (`1.5`). Lo hace `toPrintableString()`.

**Contexto.** `InterpreterContext` inmutable copy-on-write con parent chain: `declareVariable` devuelve contexto nuevo (shaddea), `assignVariable` camina hacia arriba, reconstruye la cadena dueña y devuelve el resultado (nada de lo anterior muta). Los executors devuelven `StatementResult(sideEffects, newContext)` y `DefaultInterpreter` teje el contexto a través de los statements.

**División por cero:** guard explícito en `BinaryOperationEvaluator` antes de aplicar la regla → `DivisionByZero` (con `Double` sería `Infinity` silencioso).

**Tipos declarados:** `let x: number = "hola";` se declara sin chequear el TYPE. Chequear initializer vs anotación es trabajo del type-checker; el intérprete confía en el programa validado.

---

## Forma del árbol que consume

Igual que el resto del pipeline: nombres = reglas. Ojo con:

- `LeftRuleHandler` **siempre envuelve**: `expression` sin operador queda con **un solo hijo** y `BinaryOperationEvaluator` lo trata como passthrough.
- El operador de un binario está en `children[1]` (hoja `OPERATOR` capturada), no en el token del nodo.
- En un `variable`, el id llega como hijo `"ID"` (captura de seq), no como `"identifier"`.
- Un call anidado dentro de otra expresión es válido para el parser: `1 + println(x)` falla recién en runtime.

---

## Tests

| Archivo | Qué cubre |
|---|---|
| `InterpreterContextTest` | scopes, shadowing, assign con rebuild de cadena, assign no declarada |
| `ExpressionSolverTest` | literales (incluye strip de comillas), identificador, binarios, unario, div-cero, operandos inválidos, calls anidados, errores de dispatch |
| `DefaultInterpreterTest` | threading de contexto, redeclaración, orden de efectos, fail-fast, validaciones de construcción |
| `InterpreterIntegrationTest` | `.ps` real lex→parse→interpret con asserts sobre `List<SideEffect>` |

Correr: `./gradlew :interpreter:test`.

Nota: los tests usan su propio `LanguageConfig` en código (`support/PsSupport`) con un `partial` de número que soporta decimales (`^[0-9]+(\.[0-9]*)?$`). El `partial` de `language.config.json` (`^[0-9]`) corta `1.5` en el lexer — gap preexistente del lexer, no del interpreter.

---

## Cómo extender

### Nueva expresión (ej. comparaciones)

1. Entrada en `NodeKind` + entrada en `PrintScriptMapping` con el nombre de regla del JSON.
2. `ExpressionEvaluator` nuevo con `override val kind`.
3. Registrar en `DefaultInterpreterFactory.evaluators`.
4. Si necesita tabla de tipos: reglas nuevas en `DefaultTypeConfiguration`.

### Nuevo statement (ej. asignaciones sueltas, if)

1. Igual que arriba pero con `StatementExecutor` registrado en `statementExecutors`.
2. Si el cuerpo repite statements (bloques), extraer el loop de `executeBlock` a un `fun interface BlockExecutor` y pasarlo como parámetro de `execute` — la firma actual ya reserva el lugar sin depender del intérprete concreto.
