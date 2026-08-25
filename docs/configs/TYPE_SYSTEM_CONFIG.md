# Type-system Configuration — `type-system.config.json`

Configuración declarativa del type-checker. Define **tipos**, **literales**, **operaciones** y **cómo se interpretan los nodos** del parse tree (`SyntaxNode.name`).

Se carga igual que language/grammar: interface en `common`, serializers en `infrastructure`. El type-checker **no** lee el JSON: recibe un `TypeSystemConfig` ya armado.

```kotlin
val typeSystem = JSONTypeSystemConfigReader.read(Path.of("type-system.config.json"))
val checker = DefaultTypeCheckerFactory.create(typeSystem)
```

El archivo real está en `infrastructure/src/main/resources/type-system.config.json`.

---

## Estructura

```json
{
  "types": ["number", "string"],
  "literals": {
    "NUMBER_LITERAL": "number",
    "STRING_LITERAL": "string"
  },
  "operations": [
    { "op": "+", "operands": ["number", "number"], "result": "number" }
  ],
  "nodes": {
    "variable": {
      "kind": "declaration",
      "id": "ID",
      "declaredType": "TYPE",
      "expression": "expression"
    }
  }
}
```

| Campo | Qué es |
|---|---|
| `types` | Nombres de tipo del lenguaje |
| `literals` | `Token.type` → tipo |
| `operations` | Tabla `(op, operandos) → resultado` |
| `nodes` | `SyntaxNode.name` → kind + nombres de hijos |

Al construir `TypeSystemConfig`, todo tipo citado en `literals` o `operations` (operandos y `result`) tiene que existir en `types`. Si no, `IllegalArgumentException`.

No hay validación cruzada con grammar/language todavía (no hay `ConfigBundle`).

---

## `types`

Lista de strings. V1: `"number"`, `"string"`. Un tipo nuevo (`boolean`) se agrega acá — y en tokens/grammar si el lenguaje lo escribe.

---

## `literals`

Mapa **tipo de token** → tipo. El handler `literal` mira `node.token.type`, no el nombre de la regla.

```json
"NUMBER_LITERAL": "number"
```

Un nodo `name = "number"` con token `NUMBER_LITERAL` resuelve a `"number"` porque el kind es `literal` y el mapa dice eso.

---

## `operations`

Lista de entradas:

| Campo | Tipo | Default |
|---|---|---|
| `op` | string (el lexema: `"+"`, `"*"`) | — |
| `operands` | lista **ordenada** de tipos | — |
| `result` | tipo | — |
| `commutative` | boolean | `true` si se omite |

Matcher:

1. Orden exacto `(op, T1, T2)`.
2. Si no hay match y `T1 != T2`, permutación `(op, T2, T1)` **solo** si `commutative` es true.

`"a" + 1` pega `string, number`. `1 + "a"` entra por permutación. Con `"commutative": false` la permutación no se prueba.

El v1 declara:

- `+` number+number → number
- `+` string+string → string
- `+` string+number → string
- `-` `*` `/` number+number → number

No hay conversiones implícitas fuera de esta tabla.

---

## `nodes`

Las claves son los **nombres de regla** que produce el parser. `kind` elige el handler. El resto de campos son nombres de **hijos** (`child(...)`), no de tokens del lexer.

| Kind | Handler | Campos extra |
|---|---|---|
| `declaration` | `DeclarationHandler` | `id`, `declaredType`, `expression` |
| `expression` | `ExpressionStmtHandler` | `expression` |
| `binary-or-primary` | `BinaryOrPrimaryHandler` | — (3 hijos = binario; 1 hijo = primario) |
| `primary` | `PrimaryHandler` | — (un hijo) |
| `call` | `CallHandler` | `callee`, `args` (lista de nombres de hijo) |
| `group` | `GroupHandler` | `expression` |
| `literal` | `LiteralHandler` | — |
| `identifier` | `IdentifierHandler` | — |

El Kotlin **no** hardcodea `"variable"` ni `"expression"`. Statement nuevo = producción en grammar + entrada acá. Solo un *kind* nuevo requiere código.

V1 (el JSON de resources): `variable` (declaration), `expression-stmt` (expression), `expression`/`term` (binary-or-primary), `factor` (primary), `call`, `group`, `number`/`string` (literal), `identifier`.

---

## Serializers

Igual que grammar/language:

- El dominio (`TypeSystemConfig`, `Operation`, `NodeConfig`) vive en `common` y **no** tiene `@Serializable`.
- Surrogates en `infrastructure/serializer/config`: `TypeSystemConfigSerializer`, `OperationSerializer`, `NodeConfigSerializer`.
- `JSONTypeSystemConfigReader` implementa `TypeSystemConfigReader`.

---

## Qué no hace

- No es la gramática ni el lexer.
- No infiere tipos (el lenguaje exige anotación).
- No valida aridad ni el nombre de `println`.
- El type-checker no abre este archivo: lo lee `infrastructure` y se lo pasa ya validado.
