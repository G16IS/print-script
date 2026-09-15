# PrintScript
### Trabajo Práctico - Segunda parte
*Ingeniería de sistemas — 2026*

## Descripción

En base al trabajo realizado hasta ahora sobre PrintScript, se pide extender la funcionalidad para soportar una nueva versión.

El CLI deberá permitir seleccionar la versión que se desea ejecutar o validar. En el caso de usar funcionalidades que no están en la versión seleccionada el parser o intérprete deben indicar el error en el output.

### PrintScript 1.1

Esta nueva versión del lenguaje es una simple extensión de la versión 1.0.

Las nuevas funcionalidades que se incluyen son:

- **Declaración de variables**
    - Constantes declaradas con el keyword `const`.
- **Soporte solo de los siguientes tipos básicos:**
    - `boolean`
- **Expresiones condicionales:**
    - Con la keyword `if`
    - Solo con variables `boolean` como argumento.
    - Los bloques siempre deben abrir y cerrar con `{` y `}`, respectivamente.
    - Soporta un `else`.
    - No soporta `else if`.
- **readInput**: Una expresión que no pertenece a TypeScript, que permite ingresar valores. Su sintaxis es equivalente a una función de nombre `readInput` en TypeScript. Esta función de lectura recibe como argumento un string que es el mensaje que se imprimirá previo al ingreso del valor. Dicha función tendrá como tipo de retorno un tipo compatible con el de la variable a la que se asignará el resultado (o string si se llama dentro como arg del `printLn`), pudiendo esta ser `string`, `number` o `boolean`. Si el valor provisto no puede ser interpretado como el type correspondiente, por ejemplo se espera un `boolean` pero llega `"Hola"`, entonces la ejecución falla.
    - El valor será provisto al programa por el medio correspondiente según el ambiente de ejecución.
        - En el CLI se requerirá el valor por "Standard input", como una línea.
        - En el uso programático del interpreter deberá existir un mecanismo para proveer los valores.
- **readEnv**: Una expresión que no pertenece a TypeScript, que permite ingresar valores. Recibe como argumento un string que es el nombre de la variable de ambiente que se quiere leer. Dicha función tendrá como tipo de retorno un tipo compatible con el de la variable a la que se asignará el resultado (o string si se llama dentro como arg del `printLn`), pudiendo esta ser `string`, `number` o `boolean`. Si el valor provisto no puede ser interpretado como el type correspondiente, por ejemplo se espera un `boolean` pero llega `"Hola"`, entonces la ejecución falla.

## Herramientas

Por las nuevas características del lenguaje también se agregan algunas características a las herramientas.

### Formatter

Las nuevas reglas que se deben soportar para este trabajo son:

- Manejar el indentado dentro de los bloques de `if`
    - El contenido dentro de un bloque debe tener una indentación de n espacios " " con respecto a fuera del bloque.
    - El valor de "n" debe ser configurable.
- La llave que abre un bloque de `if` debe estar en la misma línea que el `if`.

### Static code analyser

Las reglas que se deben soportar para este trabajo son:

- La función `readInput` solo puede llamarse con un identificador o un literal pero no con una expresión. Esto puede estar prendido o apagado.

## Entregables

- Actualizar los entregables previos para agregar los nuevos requerimientos.
