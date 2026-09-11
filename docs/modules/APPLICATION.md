# Módulo `application`

Dependencias: `common`, `lexer`, `parser`, `type-checker`, `interpreter`, `formatter`, `linter`. **No** depende de `:infrastructure` (I/O se inyecta: `CodeReader` + configs ya parseadas). Tests: `testImplementation` de infrastructure.

Capa de orquestación: arma lexer + parser + type-checker / interpreter / formatter / linter y expone los casos de uso. No debería contener algoritmos de matching, gramática, tipos ni whitespace.

Paquete de producción: `usecases`. Tests: `edu.austral.dissis`. El resto del repo es `printscript`.

---

## Cuándo tocarlo

- Cablear CLI, interpreter o linter
- Casos de uso de format / format-check
- Tests de integración contra archivos `.ps`
- **No** para cambiar precedencia o keywords — JSON + lexer/parser

---

## Mapa de archivos

```
application/src/main/kotlin/
  usecases/InterpretCode.kt       lex + parse + type-check
  usecases/ExecuteCode.kt         type-check + interpreter → SideEffect
  usecases/ParseProgram.kt        lex + parse (interno; recibe CodeReader)
  usecases/FormatCode.kt          formatCode(...)
  usecases/CheckFormat.kt         checkFormat(...) — `Formatter.check`, lista mismatches
  usecases/LintProgram.kt         lint sobre el árbol parseado
  usecases/LoadFormatter.kt       arma Formatter desde configs ya parseadas

application/src/test/
  kotlin/edu/austral/dissis/
    usecases/InterpretCodeTest.kt
    usecases/ExecuteCodeTest.kt
    usecases/FormatCodeTest.kt
    usecases/CheckFormatTest.kt
    testing/
      ParseExample.kt              carga grammar + type-system JSON + LanguageConfig de test
      FormatExample.kt             grammar + LanguageConfig para format/check
      PrintScriptLanguage.kt       LanguageConfig (mismo order que el JSON)
      ast/
        AstBuilder.kt              DSL node("variable") { … }
        AstSpec.kt
        AstAssert.kt               assertAst(program) { … }
        AstRenderer.kt             dump del árbol en mensajes de fallo
  resources/examples/
    declarations_and_prints.ps
    binary_expression.ps
    string_literal.ps
    type_mismatch.ps
    undeclared_variable.ps
    redeclaration.ps
    unformatted_expression.ps
    formatted_expression.ps
    unformatted_declaration.ps
    formatted_declaration.ps
```

`grammar.config.v1.json` y `type-system.config.v1.json` de test salen del **classpath de infrastructure**. Los `.ps` sí son de application.

---

## Caso de uso: `interpretCode`

```kotlin
fun interpretCode(
    langConfig: LanguageConfig,
    grammar: Grammar,
    typeSystem: TypeSystemConfig,
    reader: CodeReader,
): Report<SyntaxProgram, TypeError>
```

Pasos:

1. `ParseProgram.parse(langConfig, grammar, reader)` — lexer + parser hasta EOF
2. `DefaultTypeCheckerFactory.create(typeSystem).check(program)` — ese `Report` es el valor de retorno

`TypeError` acá es el data class de `:type-checker`, no el sealed de `common`.

Las tres configs llegan **ya construidas**. Application no lee JSON en el caso de uso (sí `ParseExample` en tests).

No hay ejecución de `println` en este caso de uso. “Interpret” acá = lex + parse + type-check. La ejecución está en `ExecuteCode` (CLI `run`).

No hay `Main.kt` acá: el CLI es `:cli` (`printscript.cli.MainKt`).

---

## Casos de uso: `formatCode` / `checkFormat`

No type-chequean: application parsea con `ParseProgram` y formatea; el type-checker no entra en este camino (el formatter no lo pide ni lo sabe). `LoadFormatter` **no** lee archivos: recibe configs ya parseadas (JSON/YAML los lee el CLI vía readers de infrastructure).

```kotlin
fun formatCode(...): Result<String, FormatError>
fun checkFormat(...): Report<Unit, FormatError>
```

`formatCode` / `checkFormat` reciben un `Formatter` ya construido (`LoadFormatter.load` con configs parseadas) y un `CodeReader`. No leen JSON ni YAML.

`formatCode` formatea el árbol. Con las rules v1: `1+2;` → `"1 + 2;\n"`.

`checkFormat` llama `Formatter.check` (mismatches puntuales, con `line:col`). Normaliza `\r\n`; no hace `trimEnd` (el newline tras `;` es parte del contrato). Recibe el `source` aparte (el caller lee el archivo).

Tests:

- `FormatCodeTest` — `unformatted_expression.ps` (`1+2;`) → `Ok("1 + 2;\n")`; `unformatted_declaration.ps` (`let x:number=1;`) → `Ok("let x : number = 1;\n")`
- `CheckFormatTest` — `1+2;` y `let x:number=1;` no son ok; `1 + 2;` y `let x : number = 1;` sí

---

## Tests de integración

`ParseExample.parse("foo.ps")`:

- Lenguaje: `PrintScriptLanguage.config()` (**no** el JSON del lexer)
- Gramática: resource `grammar.config.v1.json`
- Type-system: resource `type-system.config.v1.json`
- Path: resource `examples/foo.ps` resuelto a `File` absoluto

`PrintScriptLanguage` duplica las reglas de `language.config.v1.json` con el mismo `order` (keywords primero). Si agregás un token, actualizá JSON **y** esta clase **y** el `PrintScriptLanguage` del lexer (y `PsSupport` del interpreter).

`InterpretCodeTest` aserta la **forma** del árbol, no locations:

- `declarations_and_prints.ps` — dos `let` + dos `println`
- `binary_expression.ps` — `1 + 2 * 3` (el `*` queda dentro del `term` derecho)
- `string_literal.ps` — value `"\"hola\""` (comillas incluidas)
- `type_mismatch.ps` / `undeclared_variable.ps` / `redeclaration.ps` — el `Report` no es ok; el mensaje de tipo está en `errors`

DSL:

```kotlin
assertAst(program) {
    node("variable") {
        node("ID", "x")
        node("TYPE", "number")
        node("expression") { node("term") { node("number", "1") } }
    }
}
```

Compara `name`, `value` si se pasó, y **cantidad y orden de hijos**. Un hijo extra (por ejemplo dejar de wrappear `LeftRule`) rompe el test. Por eso no aplanes `expression → term → number`.

El helper privado `printCall("pepe")` documenta el shape de `println` en el árbol v1.

---

## Ejemplos `.ps`

`declarations_and_prints.ps`:

```
let pepe: string = "Hello, World!";
let pepa: number = 42;
println(pepe);
println(pepa);
```

`binary_expression.ps`:

```
let x: number = 1 + 2 * 3;
println(x);
```

`string_literal.ps`:

```
let greeting: string = "hola";
println(greeting);
```

Son el contrato de integración del lenguaje v1. Si cambiás la gramática de forma visible en el árbol, actualizá estos tests (no al revés: no cambies el árbol solo para que el DSL quede más lindo, salvo que el wrap de `LeftRule` deje de ser intencional).

---

## Cómo extender

### CLI

Ver [CLI.md](CLI.md). Los comandos de `:cli` cargan configs, llaman a estos use cases y presentan el resultado.

### Execute / interpreter

`ExecuteCode.execute(...)` parsea, type-chequea y, si el report es ok, `DefaultInterpreterFactory.create().interpret(InterpreterContext(), program)`. Si hay errores de tipo no interpreta. Ver [INTERPRETER.md](INTERPRETER.md).

### Nuevo caso de uso

Otro archivo en `usecases/` (ej. solo lexear, o ejecutar). Dejá `interpretCode` como orquestación del pipeline. Execution va al interpreter; estilo al linter. No lo metas en el parser.

### Nuevo ejemplo

Archivo en `resources/examples/` + test en `InterpretCodeTest` con el DSL. Preferí ejemplos chicos que fijen **una** cosa (precedencia, string, declaraciones).

---

## Invariantes

- Application no reimplementa reglas de token ni de gramática.
- El formatter no lee archivos: `LoadFormatter` le inyecta configs ya parseadas. Paths de JSON/YAML los resuelve el CLI (`PrintScriptConfigs.load`).
- El `LanguageConfig` que usás en runtime tiene que tener el `order` que el **lexer real** espera (último = más prioritario), no el del markdown.
- `interpretCode` asume que hay statements hasta EOF. Un archivo vacío (solo whitespace) hace `peek` → `EOF` y devuelve `SyntaxProgram.empty()` sin llamar al parser. Un archivo con basura al inicio tira desde lexer o parser.
