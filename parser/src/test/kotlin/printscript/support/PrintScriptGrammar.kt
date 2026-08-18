package printscript.support

/**
 * Mirror of infrastructure/src/main/resources/grammar.config.json — keep in sync.
 */
val PrintScriptGrammar = grammar(
    "statement",
    "statement" to or("variable", "expression-stmt"),
    "variable" to seq(
        expect("LET"),
        capture("ID"),
        expect("COLON"),
        capture("TYPE"),
        expect("ASSIGN"),
        ref("expression"),
        expect("SEMICOLON")
    ),
    "expression-stmt" to seq(
        ref("expression"),
        expect("SEMICOLON")
    ),
    "expression" to left("term", "OPERATOR", "+", "-"),
    "term" to left("factor", "OPERATOR", "*", "/"),
    "factor" to or("number", "string", "identifier", "call", "group"),
    "number" to atom("NUMBER_LITERAL"),
    "string" to atom("STRING_LITERAL"),
    "identifier" to atom("ID"),
    "call" to seq(
        capture("CALL"),
        expect("LEFT_PAREN"),
        ref("expression"),
        expect("RIGHT_PAREN")
    ),
    "group" to seq(
        expect("LEFT_PAREN"),
        ref("expression"),
        expect("RIGHT_PAREN")
    )
)
