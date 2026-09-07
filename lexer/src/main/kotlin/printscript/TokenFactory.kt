package printscript

import java.util.Optional
import printscript.ast.Location
import printscript.domain.Token
import printscript.domain.TokenRule
import printscript.util.Result

object TokenFactory {
    fun create(
        rule: TokenRule,
        location: Location,
        value: String,
    ): Result.Ok<Token> =
        Result.Ok(
            Token(
                type = rule.token,
                value = if (rule.capture) Optional.of(value) else Optional.empty(),
                location = location,
            ),
        )
}
