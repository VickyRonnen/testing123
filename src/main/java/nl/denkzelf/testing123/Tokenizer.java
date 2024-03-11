package nl.denkzelf.testing123;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * This tokenizer is pragmatic, it doesn't do syntax checks for numbers, eg 0b_101010_ would be accepted.
 * But the tokenizer is fed with syntactically correct Java, so 0b_101010_ will never been fed
 * This makes regexes simpler and much faster.
 * <p>
 * The Java 21 language spec can be found at https://docs.oracle.com/javase/specs/jls/se21/html/index.html
 */

public class Tokenizer {
    private final char EOF = 0xFFFF;
    @Getter
    private final String input;
    private final Set<String> keywords = initKeywords();
    private final Set<String> contextualKeywords = initContextualKeywords();
    @Getter
    private int pos = 0;
    @Getter
    private int line = 1;
    @Getter
    private int column = 1;
    private int startLine;
    private int startColumn;
    private Set operators = Set.of(
            "=", ">", "<", "!", "~", "?", ":", "->", "==", ">=", "<=", "!=", "&&", "||", "++", "--", "+", "-", "*", "/", "&", "|", "^", "%", "<<", ">>", ">>>", "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>="
    );

    /**
     * Construct Tokenizer and read input from path
     *
     * @param path
     */
    public Tokenizer(Path path) throws IOException {
//        path= Path.of("/home/vicky/IdeaProjects/tutorials/algorithms-modules/algorithms-genetic/src/main/java/com/baeldung/algorithms/ga/annealing/SimulatedAnnealing.java");
        this(Files.readString(path));
    }

    /**
     * Construct Tokenizer and set input to input
     *
     * @param input
     */
    public Tokenizer(String input) {
        this.input = input;
    }

    private Set<String> initContextualKeywords() {
        return Set.of(
                "exports",
                "module",
                "non-sealed",
                "open",
                "opens",
                "permits",
                "provides",
                "record",
                "requires",
                "sealed",
                "to",
                "transitive",
                "uses",
                "var",
                "when",
                "with",
                "yield"
        );
    }

    private Set<String> initKeywords() {
        return Set.of(
                "_",
                "abstract",
                "assert",
                "boolean",
                "break",
                "byte",
                "case",
                "catch",
                "char",
                "class",
                "const",
                "continue",
                "default",
                "do",
                "double",
                "else",
                "enum",
                "extends",
                "final",
                "finally",
                "float",
                "for",
                "goto",
                "if",
                "implements",
                "import",
                "instanceof",
                "int",
                "interface",
                "long",
                "native",
                "new",
                "package",
                "private",
                "protected",
                "public",
                "return",
                "short",
                "static",
                "strictfp",
                "super",
                "switch",
                "synchronized",
                "this",
                "throw",
                "throws",
                "transient",
                "try",
                "void",
                "volatile",
                "while"
        );
    }

    /**
     * get a token from the current position from input
     *
     * @return Token
     * @throws ParsingException on error
     */
    public Token nextToken() throws ParsingException {
        startLine = this.line;
        startColumn = this.column;
        char c;
        Token token;
        String value;
        while ((c = peek()) != EOF) {
            if (Character.isWhitespace(c)) {
                while (Character.isWhitespace(c)) {
                    eat(c);
                    c = peek();
                }
                continue;
            }
            token = comment();
            if (token != null) {
                return token;
            }
            token = string();
            if (token != null) {
                return token;
            }
            token = character();
            if (token != null) {
                return token;
            }
            token = number();
            if (token != null) {
                return token;
            }
            token = identifier();
            if (token != null) {
                return token;
            }


            token = separator();
            if (token != null) {
                return token;
            }
            token = operator();
            if (token != null) {
                return token;
            }
            throw new ParsingException(STR."Unexpected character '\{c}' at line: \{startLine} column: \{startColumn}");
        }
        return new Token(TokenType.EOF, EOF, startLine, startColumn);
    }

    private Token character() throws ParsingException {
        if (peek() != '\'') {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        eat('\'');
        while (peek() != '\'') {
            if (peek() == '\\') {
                sb.append(eat(peek()));
                sb.append(eat(peek()));
            } else {
                sb.append(eat(peek()));
            }
        }
        eat('\'');
        if (sb.isEmpty()) {
            throw new ParsingException(STR."Empty character literal at line: \{startLine} column: \{startColumn}");
        } else {
            return new Token(TokenType.CHARACTER, sb.toString(), startLine, startColumn);
        }

    }

    private Token number() throws ParsingException {
        if (peek() == '.' && "0123456789".indexOf(lookahead()) != 1) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        if (input.startsWith("0b", pos) || input.startsWith("0B", pos)) {
            sb.append(eat(peek()));
            sb.append(eat(peek()));
            while ("01_".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            if ("lL".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
        }
        if (input.startsWith("0x", pos) || input.startsWith("0X", pos)) {
            sb.append(eat(peek()));
            sb.append(eat(peek()));
            while ("1234567890abcdefABCDEF._pP+-".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            if ("lLdDfF".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
        }
        if (peek() == '0' && "01234567_".indexOf(lookahead()) != -1) {
            sb.append(eat(peek()));
            while ("01234567_".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            if ("lL".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
        }
        if ("0123456789.".indexOf(peek()) != -1) {
            while ("0123456789._eE-+".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            if ("lLdDfF".indexOf(peek()) != -1) {
                sb.append(eat(peek()));
            }
            return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
        }
        return null;
    }

    private Token comment() throws ParsingException {
        StringBuffer sb = new StringBuffer();
        if (input.startsWith("//", pos)) {
            while (peek() != '\n' && peek() != EOF) {
                sb.append(eat(peek()));
            }
            return new Token(TokenType.SINGLELINECOMMENT, sb.toString(), startLine, startColumn);
        } else if (input.startsWith("/*", pos)) {
            sb.append(eat('/'));
            sb.append(eat('*'));
            while (peek() != EOF && !sb.toString().endsWith("*/")) {
                sb.append(eat(peek()));
            }
            if (!sb.toString().endsWith("*/")) {
                throw new ParsingException(STR."Invalid comment at line: \{startLine} column: \{startColumn}");
            }
            return new Token(TokenType.MULTILINECOMMENT, sb.toString(), startLine, startColumn);
        }
        return null;
    }

    private Token string() throws ParsingException {
        StringBuffer sb = new StringBuffer();
        char c = peek();
        if (c != '"') {
            return null;
        }
        if (input.startsWith("\"\"\"", pos)) {
            eat('"');
            eat('"');
            eat('"');
            while (peek() != '\n' && Character.isWhitespace(peek())) {
                eat(peek());
            }
            eat('\n');
            while (!(input.startsWith("\"\"\"", pos))) {
                if (peek() == '\\') {
                    sb.append(eat(peek()));
                    sb.append(eat(peek()));
                }
                sb.append(eat(peek()));
            }
            eat('"');
            eat('"');
            eat('"');
            return new Token(TokenType.TEXTBLOCK, sb.toString().stripIndent(), startLine, startColumn);
        } else {
            eat('"');
            while ((c = peek()) != EOF && c != '"') {
                if (c == '\\') {
                    sb.append(eat(c));
                    c = peek();
                    sb.append(eat(c));
                } else {
                    sb.append(eat(c));
                }
            }
            eat('"');
            return new Token(TokenType.STRING, sb.toString(), startLine, startColumn);
        }
    }

    private Token operator() throws ParsingException {
        StringBuffer sb = new StringBuffer();
        while (peek() != EOF && operators.contains(sb.toString() + peek())) {
            sb.append(eat(peek()));
        }
        if (sb.isEmpty()) {
            return null;
        } else {
            return new Token(TokenType.OPERATOR, sb.toString(), startLine, startColumn);
        }
    }

    private Token separator() throws ParsingException {
        StringBuffer sb = new StringBuffer();
        char c = peek();
        if (input.startsWith("...", pos)) {
            eat('.');
            eat('.');
            eat('.');
            sb.append("...");
        } else if (input.startsWith("::", pos)) {
            eat(':');
            eat(':');
            sb.append("::");
        } else if (c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']' || c == ';' || c == ',' || c == '@' || c == '.') {
            sb.append(eat(c));
        }
        String value = sb.toString();
        if (value.isEmpty()) {
            return null;
        } else {
            return new Token(TokenType.SEPARATOR, value, startLine, startColumn);
        }

    }

    private Token identifier() throws ParsingException {
        StringBuffer sb = new StringBuffer();
        char c = peek();
        if (Character.isJavaIdentifierStart(c)) {
            while ((c = peek()) != EOF && Character.isJavaIdentifierPart(c)) {
                sb.append(eat(c));
            }
        }
        //Special case for non-sealed it is 1 token, not non, -, sealed
        if (input.startsWith("-sealed", pos)) {
            sb.append(eat('-'));
            sb.append(eat('s'));
            sb.append(eat('e'));
            sb.append(eat('a'));
            sb.append(eat('l'));
            sb.append(eat('e'));
            sb.append(eat('d'));
        }
        String value = sb.toString();
        if (value.isEmpty()) {
            return null;
        }
        if (keywords.contains(value)) {
            return new Token(TokenType.KEYWORD, value, startLine, startColumn);
        } else if (contextualKeywords.contains(value)) {
            return new Token(TokenType.CONTEXTUALKEYWORD, value, startLine, startColumn);
        } else {
            return new Token(TokenType.IDENTIFIER, value, startLine, startColumn);
        }
    }


    /**
     * Eats the current character from input and advance to the next input position
     * line is incremented when it is a linefeed, then column is set to 1,
     * otherwise column is incremented.
     *
     * @param c the current character
     * @return the current character
     * @throws ParsingException when EOF
     */
    private char eat(char c) throws ParsingException {
        if (pos >= input.length()) {
            throw new ParsingException(STR."Unexpected eof at line: \{startLine} column: \{startColumn}");
        }
        if (c != input.charAt(pos)) {
            throw new ParsingException(STR."Expected char '\{c}' but got '\{input.charAt(pos)}' at line: \{startLine} column: \{startColumn}");
        }
        if (c == '\n') {
            line++;
            column = 0;
        }
        column++;
        pos++;
        return c;
    }

    /**
     * @return the current char from input without advancing
     */
    private char peek() {
        if (pos < input.length()) {
            return input.charAt(pos);
        } else {
            return EOF;
        }
    }

    /**
     * @return the next char from input without advancing
     */
    private char lookahead() {
        if ((pos + 1) < input.length()) {
            return input.charAt(pos + 1);
        } else {
            return EOF;
        }
    }
}
