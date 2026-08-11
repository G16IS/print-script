package lexer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import printscript.Lexer
import printscript.domain.Token
import printscript.domain.TokenType

class TokenLister {
    fun listTokens(tokenStream: Lexer): ImmutableList<Token> {
        val tokenList: MutableList<Token> = mutableListOf()

        while (true) {
            val token: Token = tokenStream.nextToken()
            tokenList.add(token)
            if (token.type == TokenType.EOF) return tokenList.toImmutableList()
        }
    }
}
