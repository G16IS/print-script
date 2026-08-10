package lexer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import printscript.lexer.Lexer
import printscript.lexer.Token
import printscript.lexer.TokenType

class TokenLister {
    fun listTokens(tokenStream: Lexer): ImmutableList<Token>{
        val tokenList: MutableList<Token> = mutableListOf()

        while (true){
            val token: Token = tokenStream.nextToken()
            tokenList.add(token)
            if (token.type is TokenType.Eof) return tokenList.toImmutableList()
        }
    }
}
