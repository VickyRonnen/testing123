package nl.denkzelf.testing123;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Main {
    public static void main(String[] args) throws IOException, ParsingException {
        long start = System.currentTimeMillis();
        if (args == null || args.length == 0) {
            log.error("Specify directory or file");
            System.exit(1);
        }
        processFiles(args[0]);
        long end = System.currentTimeMillis();
        log.info("time: {}", end - start);
    }

    private static void processFiles(String path) throws IOException, ParsingException {
        List<Path> files = Files.walk(Path.of(path)).filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".java")).toList();

        for (Path file : files) {
            new Parser(file).parse();
        }
    }
}
