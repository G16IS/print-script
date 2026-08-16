package printscript

import printscript.ast.Location
import printscript.domain.Token
import printscript.domain.TokenRule
import java.util.Optional

object TokenFactory {
    fun create(rule: TokenRule, location: Location, value: Optional<String>): Token {
        return Token(
            type = rule.token,
            value = if (rule.capture) value else Optional.empty(),
            location = location,
        )
    }
}
