<h1 align="center">PrintScript G16</h1>

<p align="center">
  Un intérprete de <strong>PrintScript</strong> construido como un pipeline de análisis:<br>
  del archivo <code>.ps</code> al árbol de sintaxis, y de ahí hacia adelante.
</p>

<p align="center">
  <a href="https://github.com/G16IS/print-script/actions/workflows/tests.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/G16IS/print-script/tests.yml?branch=main&style=for-the-badge&label=tests&logo=gradle" alt="Tests">
  </a>
  <a href="https://github.com/G16IS/print-script/actions/workflows/lint.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/G16IS/print-script/lint.yml?branch=main&style=for-the-badge&label=lint&logo=checkmarx" alt="Lint">
  </a>
  <a href="https://github.com/G16IS/print-script/actions/workflows/format.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/G16IS/print-script/format.yml?branch=main&style=for-the-badge&label=format&logo=prettier" alt="Format">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/JVM-21-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="JVM 21">
  <img src="https://img.shields.io/badge/Gradle-monorepo-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle">
</p>

---

## Qué es esto

PrintScript es el lenguaje de script de la materia. Este repo es el compilador/intérprete del **grupo 16**: leés un programa, lo desarmás en piezas, armás el árbol y (más adelante) lo validás y lo ejecutás.

Hoy el camino llega hasta el **árbol de sintaxis**. El type-checker, el intérprete, el linter y el formatter del lenguaje están en el mapa, pero todavía no están cableados.

```printscript
let pepe: string = "Hello, World!";
let pepa: number = 42;

println(pepe);
println(1 + 2 * 3);
```

Un programa típico declara variables con tipo, imprime, y combina números (o strings) con la aritmética de siempre.

---

## Cómo funciona

El diseño no es un monolito que “entiende PrintScript”. Es un **pipeline**: cada etapa hace una sola cosa y le pasa el resultado a la siguiente.

```mermaid
flowchart LR
    A["archivo .ps"] --> B["Lexer"]
    B --> C["Parser"]
    C -.-> D["Type checker"]
    D -.-> E["Interpreter"]

    style D stroke-dasharray: 5 5
    style E stroke-dasharray: 5 5
```

| Etapa | Qué hace | Estado |
|---|---|---|
| **Lexer** | Lee el archivo carácter a carácter y lo corta en tokens (`let`, un identificador, un `42`, un `+`…) | Listo |
| **Parser** | Arma el árbol de sintaxis statement por statement, respetando precedencia (`*` / `/` ganan a `+` / `-`) | Listo |
| **Type checker** | Chequea tipos, redeclaraciones y variables que no existen | En camino |
| **Interpreter** | Ejecuta el programa (`println`, expresiones, más adelante control de flujo) | A futuro |
| **Linter / formatter** | Estilo y pretty-print sobre el árbol — no forman parte de la cadena de ejecución | A futuro |

Dos ideas que recorren todo el proyecto:

1. **Streaming.** Ni el lexer ni el parser se comen el archivo de una. Van leyendo de a poco: caracteres → tokens on demand → un statement por llamada.
2. **El lenguaje no está hardcodeado.** Qué es un token y qué es una producción válida vive en JSON. El Kotlin es el motor; PrintScript es la config.

```
language.config.json     →  cómo se reconocen las palabras del lenguaje
grammar.config.json      →  cómo se combinan esas palabras en un programa
```

Agregar un keyword o una forma de statement, en el caso feliz, es editar esos archivos — no reescribir el parser.

---

## El lenguaje, hoy

PrintScript **v1**, el que describe la config actual:

| Se puede | Todavía no |
|---|---|
| `let nombre: string \| number = …;` | `if`, llaves, bloques |
| `println(expresión);` | Varios argumentos en `println` |
| Literales, identificadores, `+ - * /` y paréntesis | Asignaciones sueltas (`x = 1;`) |
| `*` y `/` con más precedencia que `+` y `-` | |
| `+` entre strings = concatenación *(a nivel semántico)* | |

El parser ya sabe evaluar repeticiones (`repeat`), pensado para cuando lleguen los bloques. La gramática v1 todavía no los usa.

---

## Anatomía del repo

Monorepo Gradle. Cada carpeta es una pieza con un rol chico:

```
.ps  →  infrastructure  →  lexer  →  parser  →  application
              ↑                 ↑         ↑
           configs JSON      common (contratos compartidos)
```

- **`common`** — el vocabulario: tokens, gramática, árbol, posiciones. Nadie habla con nadie sin pasar por acá.
- **`infrastructure`** — lee los JSON y el archivo fuente. Es la única pieza que sabe de disco y de serialización.
- **`lexer` / `parser`** — motores genéricos. No conocen `let` ni `println`; conocen reglas.
- **`application`** — arma el pipeline y lo corre contra un path. Hoy eso se llama `interpretCode`, aunque todavía no “interpreta”: lexea y parsea.
- **`semantic`** — el chequeo de tipos vive acá, desconectado del pipeline. Se va a migrar a un type-checker sobre el árbol nuevo.
- **`build-logic`** — calidad del *código Kotlin* (ktlint + detekt). No es el linter de PrintScript.

El detalle de cada módulo, invariantes y recetas de extensión está en [`docs/CONTEXT.md`](docs/CONTEXT.md).

---

## Correrlo

Hace falta **JDK 21**.

```bash
./gradlew test          # todos los módulos
./gradlew ktlintCheck   # estilo (lo mismo que el badge Format)
./gradlew detekt        # análisis estático (lo mismo que el badge Lint)
```

CI corre esas tres cosas en cada push / PR a `main`: [tests](.github/workflows/tests.yml), [lint](.github/workflows/lint.yml) y [format](.github/workflows/format.yml).

---

## Docs

| Para… | Empezá por |
|---|---|
| Entender el proyecto entero | [`docs/CONTEXT.md`](docs/CONTEXT.md) |
| Cambiar tokens / keywords | [`docs/LANGUAGE_CONFIG.md`](docs/LANGUAGE_CONFIG.md) |
| Cambiar la sintaxis | [`docs/GRAMMAR_CONFIG.md`](docs/GRAMMAR_CONFIG.md) |
| Un módulo en particular | [`docs/modules/`](docs/modules/) |

---

<p align="center">
  <sub>Grupo 16 · Ingeniería de Sistemas · Universidad Austral</sub>
</p>
