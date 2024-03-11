package nl.denkzelf.testing123;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static nl.denkzelf.testing123.TokenType.EOF;

@Slf4j
public class Parser {
    private final Path path;
    private List<Token> tokens = new ArrayList<>();
    private Tokenizer tokenizer;

    public Parser(Path path) throws IOException {
        this.path = path;
        tokenizer = new Tokenizer(path);

    }

    public void parse() throws ParsingException {
        log.info("Parsing file {}", path);
        try {
            Token token;
            while ((token = tokenizer.nextToken()).getTokenType() != EOF) {
                tokens.add(token);
            }
            tokens.add(token);
        } catch (ParsingException e) {
            tokens.forEach(token -> log.error("Token: {}", token));
            log.info("Parsing file {}", path);
            throw e;
        }
    }
}
