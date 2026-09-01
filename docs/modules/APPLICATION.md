# Módulo `application`

Dependencias: `common`, `lexer`, `parser`, `type-checker`, `infrastructure`.

Capa de orquestación: arma readers + lexer + parser + type-checker y expone el caso de uso. No debería contener algoritmos de matching, gramática ni de tipos.

Paquete de producción: `usecases` (`InterpretCode`). Tests: `edu.austral.dissis`. El resto del repo es `printscript`. No depende de `:interpreter`.

---

## Cuándo tocarlo

- Cablear CLI, interpreter o linter
- Tests de integración contra archivos `.ps`
- **No** para cambiar precedencia o keywords — JSON + lexer/parser

---

## Mapa de archivos

```
application/src/main/kotlin/
  usecases/InterpretCode.kt       interpretCode(...)

application/src/test/
  kotlin/edu/austral/dissis/
    usecases/InterpretCodeTest.kt
    testing/
      ParseExample.kt              carga grammar + type-system JSON + LanguageConfig de test
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
```

`grammar.config.json` y `type-system.config.json` de test salen del **classpath de infrastructure**. Los `.ps` sí son de application.

---

## Caso de uso: `interpretCode`

```kotlin
fun interpretCode(
    langConfig: LanguageConfig,
    grammar: Grammar,
    typeSystem: TypeSystemConfig,
    path: String
): SyntaxProgram
```

Pasos:

1. `FileCodeReader(path)` — path de filesystem
2. `DefaultLexerFactory.create(codeReader, langConfig)`
3. `DefaultParserFactory.create(grammar)`
4. `while (lexer.peek(null).type != "EOF")` → `parser.parseNextStatement(lexer, program)`
5. `DefaultTypeCheckerFactory.create(typeSystem).check(program)`
6. Si el `Report` no es ok → `error("El chequeo de tipos falló:…")` con mensaje y `line:col`
7. Si no, devuelve el `SyntaxProgram`

Las tres configs llegan **ya construidas**. Application no lee JSON en el caso de uso (sí `ParseExample` en tests).

No hay ejecución de `println`. “Interpret” acá = lex + parse + type-check. El módulo `:interpreter` existe y sabe emitir `PrintEffect`; no está en las deps de este módulo.

No hay `Main.kt` ni CLI (`args[0]`, flags de versión, etc.).

---

## Tests de integración

`ParseExample.parse("foo.ps")`:

- Lenguaje: `PrintScriptLanguage.config()` (**no** el JSON del lexer)
- Gramática: resource `grammar.config.json`
- Type-system: resource `type-system.config.json`
- Path: resource `examples/foo.ps` resuelto a `File` absoluto

`PrintScriptLanguage` duplica las reglas de `language.config.json` con el mismo `order` (keywords primero). Si agregás un token, actualizá JSON **y** esta clase **y** el `PrintScriptLanguage` del lexer (y `PsSupport` del interpreter).

`InterpretCodeTest` aserta la **forma** del árbol, no locations:

- `declarations_and_prints.ps` — dos `let` + dos `println`
- `binary_expression.ps` — `1 + 2 * 3` (el `*` queda dentro del `term` derecho)
- `string_literal.ps` — value `"\"hola\""` (comillas incluidas)
- `type_mismatch.ps` / `undeclared_variable.ps` / `redeclaration.ps` — `interpretCode` tira `IllegalStateException` con el mensaje de tipo

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

Crear `Main.kt`: cargar configs (resources o paths), `interpretCode`, imprimir el árbol o errores. `FileCodeReader` no cierra el file; para un one-shot está bien.

### Cablear interpreter

Ver [INTERPRETER.md](INTERPRETER.md). El type-checker ya corre sobre `SyntaxProgram` después del parser. Falta: `implementation(project(":interpreter"))` y, si el report es ok, `DefaultInterpreterFactory.create().interpret(InterpreterContext(), program)`.

### Nuevo caso de uso

Otro archivo en `usecases/` (ej. solo lexear, o ejecutar). Dejá `interpretCode` como orquestación del pipeline. Execution va al interpreter; estilo al linter. No lo metas en el parser.

### Nuevo ejemplo

Archivo en `resources/examples/` + test en `InterpretCodeTest` con el DSL. Preferí ejemplos chicos que fijen **una** cosa (precedencia, string, declaraciones).

---

## Invariantes

- Application no reimplementa reglas de token ni de gramática.
- El `LanguageConfig` que usás en runtime tiene que tener el `order` que el **lexer real** espera (primero = más prioritario), igual que `language.config.json`.
- `interpretCode` asume que hay statements hasta EOF. Un archivo vacío (solo whitespace) hace `peek` → `EOF` y devuelve `SyntaxProgram.empty()` sin llamar al parser. Un archivo con basura al inicio tira desde lexer o parser.
