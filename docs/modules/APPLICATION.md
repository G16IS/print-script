# Módulo `application`

Dependencias: `common`, `lexer`, `parser`, `semantic`, `infrastructure`.

Capa de orquestación: arma readers + lexer + parser y expone el caso de uso. No debería contener algoritmos de matching ni de gramática.

Paquete: `edu.austral.dissis` (el resto del repo es `printscript`).

---

## Cuándo tocarlo

- Cablear type-checker, CLI, interpreter o linter
- Tests de integración contra archivos `.ps`
- **No** para cambiar precedencia o keywords — JSON + lexer/parser

---

## Mapa de archivos

```
application/src/main/kotlin/
  Main.kt                          fun main() {}  — vacío
  usecases/InterpretCode.kt       interpretCode(...)

application/src/test/
  kotlin/edu/austral/dissis/
    usecases/InterpretCodeTest.kt
    testing/
      ParseExample.kt              carga grammar JSON + LanguageConfig de test
      PrintScriptLanguage.kt       LanguageConfig con order invertido
      ast/
        AstBuilder.kt              DSL node("variable") { … }
        AstSpec.kt
        AstAssert.kt               assertAst(program) { … }
        AstRenderer.kt             dump del árbol en mensajes de fallo
  resources/examples/
    declarations_and_prints.ps
    binary_expression.ps
    string_literal.ps
```

El `grammar.config.json` de test se toma del **classpath de infrastructure** (`JSONGrammarConfigReader.read(stream("grammar.config.json"))`), no hay copia en `application/src/test/resources`. Los `.ps` sí son de application.

---

## Caso de uso: `interpretCode`

```kotlin
fun interpretCode(
    langConfig: LanguageConfig,
    grammar: Grammar,
    path: String
): SyntaxProgram
```

Pasos:

1. `FileCodeReader(path)` — path de filesystem
2. `DefaultLexerFactory.create(codeReader, langConfig)`
3. `DefaultParserFactory.create(grammar)`
4. `while (lexer.peek(null).type != "EOF")` → `parser.parseNextStatement(lexer, program)`
5. Devuelve el `SyntaxProgram`

KDoc dice “Returns the validated Program or throws if semantic analysis fails”. El cuerpo **no** valida: el bloque semántico está comentado y, además, no tiparía (`analyze` espera `Program`, acá hay `SyntaxProgram`).

No hay ejecución de `println`. “Interpret” acá = lex + parse.

`Main.kt` no llama a esto. No hay CLI (`args[0]`, flags de versión, etc.).

---

## Tests de integración

`ParseExample.parse("foo.ps")`:

- Lenguaje: `PrintScriptLanguage.config()` (**no** el JSON del lexer)
- Gramática: resource `grammar.config.json`
- Path: resource `examples/foo.ps` resuelto a `File` absoluto

`PrintScriptLanguage` duplica las reglas de `language.config.json` con el `order` invertido. Comentario explícito: el resolver trata la **última** categoría como máxima prioridad. Si agregás un token, actualizá JSON **y** esta clase **y** `MockLexerFactory` del lexer.

`InterpretCodeTest` aserta la **forma** del árbol, no locations:

- `declarations_and_prints.ps` — dos `let` + dos `println`
- `binary_expression.ps` — `1 + 2 * 3` (el `*` queda dentro del `term` derecho)
- `string_literal.ps` — value `"\"hola\""` (comillas incluidas)

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

`Main.kt`: cargar configs (resources o paths), `interpretCode`, imprimir el árbol o errores. `FileCodeReader` no cierra el file; para un one-shot está bien.

### Cablear type-checker / interpreter

Ver [TYPE_CHECKER.md](TYPE_CHECKER.md) e [INTERPRETER.md](INTERPRETER.md). No descomentes el bloque de `:semantic` a ciegas: espera `Program`, acá hay `SyntaxProgram`. El type-checker tiene que caminar el parse tree.

### Nuevo caso de uso

Otro archivo en `usecases/` (ej. solo lexear, o ejecutar). Dejá `interpretCode` como orquestación del pipeline. Execution va al interpreter; estilo al linter. No lo metas en el parser.

### Nuevo ejemplo

Archivo en `resources/examples/` + test en `InterpretCodeTest` con el DSL. Preferí ejemplos chicos que fijen **una** cosa (precedencia, string, declaraciones).

---

## Invariantes

- Application no reimplementa reglas de token ni de gramática.
- El `LanguageConfig` que usás en runtime tiene que tener el `order` que el **lexer real** espera (último = más prioritario), no el del markdown.
- `interpretCode` asume que hay statements hasta EOF. Un archivo vacío (solo whitespace) hace `peek` → `EOF` y devuelve `SyntaxProgram.empty()` sin llamar al parser. Un archivo con basura al inicio tira desde lexer o parser.
