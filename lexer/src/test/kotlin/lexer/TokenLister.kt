package lexer

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import printscript.lexer.Eof
import printscript.lexer.TokenStream
import printscript.lexer.Token

class TokenLister {
    fun listTokens(tokenStream: TokenStream): ImmutableList<Token>{
        val tokenList: MutableList<Token> = mutableListOf()

        while (true){
            val token: Token = tokenStream.nextToken()
            tokenList.add(token)
            if (token.type is Eof) return tokenList.toImmutableList()
        }
    }
}
