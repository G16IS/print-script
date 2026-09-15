# Módulo `formatter`

Dependencias: `common`. Tests tiran de `infrastructure` para cargar `formatter-language.v1.0.json`.

Pretty-printer de PrintScript sobre `SyntaxProgram`. No es linter (el linter reporta; este reescribe o chequea whitespace). No ejecuta. No lee archivos: recibe configs ya parseadas. Application lo llama desde `FormatCode` / `CheckFormat`.

Pretty-printer dirigido por rules. JSON de lenguaje (rules fijas + bindings) + JSON de defaults de usuario + YAML de usuario (`type` + `enabled`/`count`, consigna). Reconstruye la puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens). Spec de las configs: [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

---

## Cuándo tocarlo

- Nueva rule de estilo que entre en space/newline → binding o rule en `formatter-language.v1.0.json`
- Nuevo *kind* de whitespace → `FormatRule` + `FormatRuleFactory` + JSON
- Cambiar el walk / `FormatPoint` / `GrammarWalker` / `Gap`
- Indent de bloques: `WalkState.indentLevel` alrededor de `{` `}` (ver receta abajo)
- Política de errores de `format` (`Result`) o `check` (`Report`)
- **No** para tokens/gramática del lenguaje
- **No** para type-check: este módulo formatea el `SyntaxProgram` que le pasen; si hay que type-chequear antes, lo decide application

---

## API pública

```kotlin
interface Formatter {
    fun format(program: SyntaxProgram): Result<String, FormatError>
    fun check(program: SyntaxProgram, source: String): Report<Unit, FormatError>
}

object DefaultFormatterFactory {
    fun create(rules: List<FormatRule>, grammar: Grammar, lexemes: TokenLexemes): Formatter
    fun createFromConfig(
        language: FormatterLanguageConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
        defaults: FormatterRulesConfig = FormatterRulesConfig(),
        grammar: Grammar,
        lexemes: TokenLexemes,
    ): Result<Formatter, FormatError>
}
```

Siempre crear por la factory. La impl es `DefaultFormatter` + `DefaultRuleRegistry`.

- `format` recorre el árbol, emite lexemas capturados e inyecta whitespace de las rules. Fail-fast en errores estructurales (`MissingLexeme`, `UnrecognizedFormatNode`).
- `check` usa el **source original** (el AST no tiene trivia). Recorre los mismos tokens que `format` (capturados y sintéticos). Un cursor busca cada lexema en el source y compara el hueco vs lo esperado. **No** usa `token.location.start` para el offset: el lexer deja esa posición *después* del primer carácter. Acumula todos los `WhitespaceMismatch` (también si lo esperado es `""` y hay extra, y el trailing) y los estructurales del seq (`UnrecognizedFormatNode`). No corta en el primero.

`FormatError` es sealed en `common` (`printscript.error`, `message` + `location`). `UnrecognizedFormatNode` (no `UnrecognizedNode`: ese nombre ya lo usan type/runtime).

El módulo **no lee archivos**. El CLI carga JSON/YAML con readers de infrastructure; `LoadFormatter` (application) arma el `Formatter` con configs ya parseadas.

---

## Strategy

```kotlin
interface FormatRule {
    fun applies(point: FormatPoint): Boolean
    fun addChar(point: FormatPoint, whitespace: Char): Int
}

interface FormatRuleFactory {
    val types: Set<String>
    fun create(spec: ResolvedFormatRule): Result<FormatRule, FormatError>
}
```

El walker imprime lexemas. Las rules no devuelven un `String` libre: el registry pregunta `addChar(point, ' ')` y `addChar(point, '\n')` y arma un `Gap` (newlines + como mucho un espacio). El cap de un espacio es invariante del combiner, no una rule (la consigna no lo configura). Si nadie aplica → `Gap.EMPTY`.

El hueco entre dos tokens es `AFTER` del anterior **más** `BEFORE` del actual (`Gap.plus`: newlines se suman, spaces se cappean a 1). Así `space-after LET` + `space-before ID` no produce dos espacios. Si el gap tiene newlines, no se emite el espacio intra-línea: el indent ocupa ese lugar. `Gap.render(indentLevel)` aplica `indentLevel * 4` espacios **después** de los newlines; en v1 el nivel es 0 y el render es identidad (`"\n"`). Al terminar el programa se emite el `AFTER` del último token (newline tras `;`).

El core es **inmutable**: `WalkState` es un `data class` (`output`, `errors`, `last`, `indentLevel`). `emit` / `FormatRuleLoader.instantiate` son `fold` + `copy`.

Puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens): `GrammarWalker` la reinyecta leyendo `SeqRule` + `TokenLexemes` (lexemas exactos de un solo matcher). Los hijos del nodo se consumen **en orden** (capturas y rule-refs); los sintéticos no avanzan el cursor. Dos rule-refs con el mismo nombre (then/else) no se pisan. `OptionalRule`: 0 hijos no emite nada; 1 hijo se camina; más es `UnrecognizedFormatNode`. Así `let x: string;` no inventa `=`. Nodos que no son `SeqRule` ni `OptionalRule` (`Or`, `Left`, `Atom`, `Repeat`) caen al walk genérico de hijos. `check` respeta `failFast=false` también en errores estructurales del seq (`UnrecognizedFormatNode`).

### Rules

**Implementaciones** (el JSON de lenguaje elige `type` + `token`):

| `type` | Rule | Qué hace |
|---|---|---|
| `space-before` / `space-after` / `space-around` | `TokenSpaceRule` | espacio en esos puntos del `token` |
| `newline-before` / `newline-after` | `TokenNewlineRule` | N newlines (`count` 0..2); `value` / `previous` opcionales |

v1 en `rules`: `space-around`+`OPERATOR`, `newline-after`+`SEMICOLON`, `space-after`+`LET`.

**Usuario** (YAML; `type` = `userType` del binding; si el archivo no existe o falta la rule, `formatter-user-defaults.json` o el `default` del binding):

| `type` | Default | Qué hace |
|---|---|---|
| `space-before-colon` | `enabled: true` | espacio antes de `:` |
| `space-after-colon` | `enabled: true` | espacio después de `:` |
| `space-around-assign` | `enabled: true` | espacio alrededor de `=` |
| `newlines-before-println` | `count: 1` (0..2) | newlines extra antes de `println` si el token previo es `;` |

Ejemplo con defaults: `let x : number = 1 + 2;\n` y `1 + 2;\n\nprintln(1);\n`.

---

## Config

Dos formas. Lectura en `infrastructure`, dominio en `common` (**no** `@Serializable`). Detalle: [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

- Lenguaje: `JSONFormatterLanguageConfigReader` + `FormatterLanguageConfigSerializer` (`rules` + `userBindings`)
- Usuario / defaults: `YAMLFormatterRulesConfigReader` / `JSONFormatterRulesConfigReader` + `FormatterRulesConfigSerializer` (`type` + `enabled`/`count`)

`FormatRuleLoader` no lee archivos: instancia `FormatterLanguageConfig` + user + defaults ya decodificadas. Merge: YAML pisa defaults JSON por `type`; si falta en ambos, el `default` del binding. YAML `type` que no está en `userBindings` → `UnknownRuleType`.

Resources: `formatter-language.v1.0.json`, `formatter-user-defaults.json`.

**TCK vs CLI.** El path TCK (`PrintScriptConfigsLoader.loadForTck`) trata el JSON de usuario como set completo: defaults vacíos, así que una key ausente queda off (no se rellena con `formatter-user-defaults.json`). El CLI sigue overlayando el YAML de `.printscript/formatter.yml` sobre esos defaults. Los casos nativos del formatter TCK en v1.1 se skipean hasta que exista el lenguaje 1.1.

---

## Mapa de archivos

```
formatter/src/main/kotlin/printscript/formatter/
  Formatter.kt
  DefaultFormatter.kt           walk puro: fold sobre WalkState inmutable
  LexemeEmit.kt                 emite lexema (format) o compara hueco (check)
  DefaultFormatterFactory.kt
  FormatPoint.kt
  Gap.kt                        newlines + spaces; plus por hueco; render(indentLevel)
  WhitespaceChars.kt            SPACE / NEWLINE
  RuleRegistry.kt               addChar → Gap (interno)
  SourceGaps.kt                 offset / position CharPosition ↔ source (para check)
  WalkState.kt                  incluye indentLevel (v1 = 0)
  NodeWalk.kt
  GrammarWalker.kt              SeqRule: cursor de hijos + sintéticos; failFast
  rules/                        TokenSpaceRule + TokenNewlineRule
  factories/
    FormatRuleFactory.kt        SpaceRuleFactory + NewlineRuleFactory
    ResolvedFormatRule.kt       type genérico + token + enabled/count
    SpaceRuleFactory.kt
    NewlineRuleFactory.kt
  config/
    FormatRuleLoader.kt         language + user + defaults → FormatRule
```

---

## Tests

Árboles fabricados (sin lex/parse):

| Clase | Qué cubre |
|---|---|
| `DefaultFormatterTest` | `1+2` → `1 + 2`; sin rules → `1+2`; `expression-stmt` + `;`; errores estructurales; seq incompleto fail-fast |
| `PrintScriptLayoutTest` | `let x : number = 1;\n`; colon sin espacios; `println` con newline extra; round-trip format→check |
| `FormatterCheckTest` | mismatches alrededor de `+`; `;` sintético; trailing; `let` sin espacios; extra líder; seq incompleto acumula |
| `GrammarWalkerTest` | dos hijos con el mismo nombre de regla en orden; space AFTER+BEFORE → un espacio |
| `GapTest` | plus (spaces cap 1, newlines suman); `render(0)` vs `render(1)` |
| `FormatRuleLoaderTest` | JSON de lenguaje; defaults JSON; fallback del binding; type desconocido; `count` inválido |
| `TokenSpaceRuleTest` / `TokenNewlineRuleTest` / `RuleRegistryTest` | `addChar`, println vs otro call, newline gana al espacio en el mismo punto |

Infrastructure: `FormatterLanguageConfigReaderTest` (resource) + `FormatterRulesConfigReaderTest` (YAML + defaults JSON).

---

## Cómo extender

1. Si entra en space/newline: `rules` o `userBindings` en `formatter-language.v1.0.json`. El YAML de usuario no cambia de forma (`type` + un value).
2. Si no: `FormatRule` + `FormatRuleFactory` en `FormatRuleFactories.defaults()`.
3. Tests de `format` / `check`. La puntuación que el parser no deja en el árbol la reinyecta `GrammarWalker`.

### Bloques indentados / `if` (cuando existan)

No hay `IndentRule`. El indent vive en `WalkState.indentLevel` y se aplica en `Gap.render`. El día que haya braces:

1. Gramática: `block` = `seq[ LEFT_BRACE, { rule: statements }, RIGHT_BRACE ]`, `statements` = `repeat statement`. `Repeat` ya cae a `emitChildren` (orden). El `Seq` reinyecta `{` `}`.
2. JSON de formatter: `newline-after` + `LEFT_BRACE`, `newline-before` + `RIGHT_BRACE` (kinds que ya existen).
3. En `GrammarWalker.emitSyntheticToken`:
   - antes de emitir `RIGHT_BRACE`: `indentLevel - 1`
   - después de emitir `LEFT_BRACE`: `indentLevel + 1`
4. Tests de un bloque. Then/else con el mismo nombre de regla ya anda por el cursor de hijos.

Un `if` sin braces (indentar el statement suelto) es el mismo `indentLevel` alrededor de ese hijo, no otro combiner. `INDENT_WIDTH` es 4 y no se configura.
