package printscript.statement

import printscript.node.AstNames

/**
 * `const <id>: <type> = <expr>;` — sólo v1.1.
 *
 * Se ejecuta igual que una declaración `let`: que no se pueda reasignar lo hace
 * cumplir el type-checker, no el intérprete.
 */
object ConstantDeclarationExecutor : DeclarationExecutor(setOf(AstNames.CONSTANT))
