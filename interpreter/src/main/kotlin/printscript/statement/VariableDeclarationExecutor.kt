package printscript.statement

import printscript.node.AstNames

/** `let <id>: <type> (= <expr>)?;` — presente en todas las versiones. */
object VariableDeclarationExecutor : DeclarationExecutor(setOf(AstNames.VARIABLE))
