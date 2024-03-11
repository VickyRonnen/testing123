package nl.denkzelf.testing123;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenizerTest {
    @Test
    @DisplayName("Should read input from file")
    void tokenizerInitFromFile() throws IOException {
        Path inputFile = Files.createTempFile(null, null);
        Files.writeString(inputFile, "Hello World\n");
        Tokenizer tokenizer = new Tokenizer(inputFile);
        assertEquals("Hello World\n", tokenizer.getInput());
        assertEquals(0, tokenizer.getPos());
        assertEquals(1, tokenizer.getLine());
        assertEquals(1, tokenizer.getColumn());
        Files.delete(inputFile);
    }

    @Test
    @DisplayName("Should read input from string")
    void tokenizerInitFromString() {
        Tokenizer tokenizer = new Tokenizer("Hello World\n");
        assertEquals("Hello World\n", tokenizer.getInput());
        assertEquals(0, tokenizer.getPos());
        assertEquals(1, tokenizer.getLine());
        assertEquals(1, tokenizer.getColumn());
    }

    @Test
    @DisplayName("nextToken should return EOF token on empty input")
    void nextTokenShouldReturnEOFToken() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.EOF, token.getType());
    }

    @Test
    @DisplayName("nextToken should return character token")
    void nextTokenShouldReturnCharacterToken() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("'a'");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.CHARACTER, token.getType());
        assertEquals("a", token.getValue());
    }

    @Test
    @DisplayName("nextToken should throw ParsingException empty character")
    void nextTokenShouldThrowExceptionOnEmptyCharacterLiteral() {
        Tokenizer tokenizer = new Tokenizer("''");
        ParsingException parsingException = assertThrows(ParsingException.class, tokenizer::nextToken);
        assertEquals("Empty character literal at line: 1 column: 1", parsingException.getMessage());
    }


    @Test
    @DisplayName("nextToken should return single quote character")
    void escaped() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("'\\\''");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.CHARACTER, token.getType());
        assertEquals("\\'", token.getValue());
    }

    @Test
    @DisplayName("nextToken should return character token for dual unicode escapes like U+1F947")
    void nextTokenShouldReturnCharacterTokenForDualUnicode() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("'\uD83E\uDD47'");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.CHARACTER, token.getType());
        assertEquals("\uD83E\uDD47", token.getValue());
    }


    @Test
    @DisplayName("nextToken should return empty string token")
    void nextTokenShouldReturnStringTokenForEmptyString() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("\"\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.STRING, token.getType());
        assertEquals("", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token")
    void nextTokenShouldReturnStringTokenForString() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("\"Hello\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.STRING, token.getType());
        assertEquals("Hello", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for escaped unicode")
    void nextTokenShouldReturnStringTokenForEscapedUnicode() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("\"H\u00ebllo\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.STRING, token.getType());
        assertEquals("Hëllo", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for escaped character")
    void nextTokenShouldReturnStringTokenForEscapedCharacter() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("\"He\\\"llo\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.STRING, token.getType());
        assertEquals("He\\\"llo", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for empty text block")
    void nextTokenShouldReturnStringTokenForEmptyTextBlock() throws ParsingException {
        String s = """
                """;
        Tokenizer tokenizer = new Tokenizer("\"\"\"\n" +
                                            "                \"\"\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.TEXTBLOCK, token.getType());
        assertEquals(s, token.getValue());
        assertEquals("", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for empty text block")
    void nextTokenShouldReturnStringTokenForNonEmptyTextBlock() throws ParsingException {
        String s = """        
                                Hello      World
                                
                World
                                """;
        Tokenizer tokenizer = new Tokenizer("\"\"\"        \n" +
                                            "                                Hello      World\n" +
                                            "                                \n" +
                                            "                World\n" +
                                            "                                \"\"\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.TEXTBLOCK, token.getType());
        assertEquals(s, token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for escaped text block")
    void nextTokenShouldReturnStringTokenForTextBlock() throws ParsingException {
        String s = """
                <html>\r
                    <body>\r
                        <p>Hello, world</p>\r
                    </body>\r
                </html>\r
                """;
        Tokenizer tokenizer = new Tokenizer("\"\"\"\n" +
                                            "                <html>\\r\n" +
                                            "                    <body>\\r\n" +
                                            "                        <p>Hello, world</p>\\r\n" +
                                            "                    </body>\\r\n" +
                                            "                </html>\\r\n" +
                                            "                \"\"\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.TEXTBLOCK, token.getType());
        assertEquals("<html>\\r\n" +
                     "    <body>\\r\n" +
                     "        <p>Hello, world</p>\\r\n" +
                     "    </body>\\r\n" +
                     "</html>\\r\n", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return string token for escaped text block")
    void nextTokenShouldReturnStringTokenForEscapedTextBlock() throws ParsingException {
        String s = """
                String text = \"""
                    The quick brown fox jumps over the lazy dog
                \""";
                """;
        Tokenizer tokenizer = new Tokenizer("\"\"\"\n" +
                                            "                String text = \\\"\"\"\n" +
                                            "                    The quick brown fox jumps over the lazy dog\n" +
                                            "                \\\"\"\";\n" +
                                            "                \"\"\"");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.TEXTBLOCK, token.getType());
        assertEquals("String text = \\\"\"\"\n" +
                     "    The quick brown fox jumps over the lazy dog\n" +
                     "\\\"\"\";\n", token.getValue());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getCol());
    }

    @Test
    @DisplayName("nextToken should return line comment")
    void nextTokenShouldReturnLineComment() throws ParsingException {
        Tokenizer tokenizer = new Tokenizer("// This is a line comment");
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.SINGLELINECOMMENT, token.getType());
        assertEquals("// This is a line comment", token.getValue());
    }

    @Test
    @DisplayName("nextToken should return multi line comment")
    void nextTokenShouldReturnMultiLineComment() throws ParsingException {
        String s = """
                /* this comment
                /* //
                /** ends here: */
                                 """;
        Tokenizer tokenizer = new Tokenizer(s);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.MULTILINECOMMENT, token.getType());
        assertEquals("/* this comment\n" +
                     "/* //\n" +
                     "/** ends here: */", token.getValue());
    }

    @Test
    @DisplayName("nextToken should throw ParsingException on unclosed multi line comment")
    void nextTokenShouldThrowExceptionUnclosedMultiLineComment1() {

        String s = """
                /* this comment
                /* //
                /** ends here:
                                         """;
        Tokenizer tokenizer = new Tokenizer(s);
        ParsingException parsingException = assertThrows(ParsingException.class, tokenizer::nextToken);
        assertEquals("Invalid comment at line: 1 column: 1", parsingException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"})
    @DisplayName("nextToken should return keyword token")
    void nextTokenShouldReturnKeywordToken(String keyword) throws ParsingException {
        Tokenizer tokenizer = new Tokenizer(keyword + "\n" + keyword);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.KEYWORD, token.getType());
        assertEquals(keyword, token.getValue());
        token = tokenizer.nextToken();
        assertEquals(TokenType.KEYWORD, token.getType());
        assertEquals(keyword, token.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"exports", "module", "non-sealed", "open", "opens", "permits", "provides",
            "record", "requires", "sealed", "to", "transitive", "uses", "var", "when", "with", "yield"})
    @DisplayName("nextToken should return contextual keyword token")
    void nextTokenShouldReturnContextualKeywordToken(String contextualKeyword) throws ParsingException {
        Tokenizer tokenizer = new Tokenizer(contextualKeyword + "\n" + contextualKeyword);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.CONTEXTUALKEYWORD, token.getType());
        assertEquals(contextualKeyword, token.getValue());
        token = tokenizer.nextToken();
        assertEquals(TokenType.CONTEXTUALKEYWORD, token.getType());
        assertEquals(contextualKeyword, token.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(", ")", "{", "}", "[", "]", ";", ",", ".", "...", "@", "::"
    })
    void testSeparators(String separator) throws ParsingException {
        Tokenizer tokenizer = new Tokenizer(separator);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.SEPARATOR, token.getTokenType());
        assertEquals(separator, token.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "!",
            "!=",
            "%",
            "%=",
            "&",
            "&&",
            "&=",
            "*",
            "*=",
            "+",
            "++",
            "+=",
            "-",
            "--",
            "-=",
            "->",
            "/",
            "/=",
            ":",
            "<",
            "<<",
            "<<=",
            "<=",
            "=",
            "==",
            ">",
            ">=",
            ">>",
            ">>=",
            ">>>",
            ">>>=",
            "?",
            "^",
            "^=",
            "|",
            "|=",
            "||",
            "~",
    })
    void testOperators(String operator) throws ParsingException {
        Tokenizer tokenizer = new Tokenizer(operator);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.OPERATOR, token.getTokenType());
        assertEquals(operator, token.getValue());
    }

    @ParameterizedTest
    @CsvSource({
            "0xf,0xf",
            "1L,1L",
            "0,0",
            "0123_4567l,0123_4567l",
            "0138,013",
            "0b1111_0000,0b1111_0000",
            "0b1111_0000l,0b1111_0000l",
            "0b1111_0000L,0b1111_0000L",
            "0x1.fff_ffeP+127f,0x1.fff_ffeP+127f",
            "7343.2342e-12d,7343.2342e-12d"
    })
    void testNumber(String number, String expected) throws ParsingException {
        Tokenizer tokenizer = new Tokenizer(number);
        Token token = tokenizer.nextToken();
        assertEquals(TokenType.NUMBER, token.getTokenType());
        assertEquals(expected, token.getValue());
    }
}
