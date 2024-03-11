package nl.denkzelf.testing123;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Token {
    private final TokenType tokenType;
    private final String value;
    private final int line;
    private final int col;


    public Token(TokenType tokenType, String value, int line, int col) {
        this.tokenType = tokenType;
        this.value = value;
        this.line = line;
        this.col = col;
    }

    public Token(TokenType tokenType, char value, int line, int col) {
        this.tokenType = tokenType;
        this.value = String.valueOf(value);
        this.line = line;
        this.col = col;
    }

    public TokenType getType() {
        return tokenType;
    }
}
