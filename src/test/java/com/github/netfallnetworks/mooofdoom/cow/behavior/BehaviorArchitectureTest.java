package com.github.netfallnetworks.mooofdoom.cow.behavior;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture invariant for issue #19: goal installation must go through
 * TemporaryBehavior so AI goals are always tag-gated and idempotently installed.
 * Direct GoalSelector.addGoal calls anywhere else reintroduce the goal-leak bug class.
 */
class BehaviorArchitectureTest {

    private static final String ALLOWED_FILE = "TemporaryBehavior.java";

    @Test
    void addGoalIsOnlyCalledInTemporaryBehavior() throws IOException {
        Path srcRoot = findSrcRoot();
        assertTrue(Files.isDirectory(srcRoot),
                "could not locate src/main/java above " + Path.of("").toAbsolutePath());

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(srcRoot)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals(ALLOWED_FILE)) continue;
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains(".addGoal(")) {
                        offenders.add(file + ":" + (i + 1));
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "GoalSelector.addGoal must only be called from TemporaryBehavior "
                        + "(tag-gated, idempotent — see issue #19). Offenders:\n"
                        + String.join("\n", offenders));
    }

    /**
     * The NeoForge unitTest runner does not use the project root as the working
     * directory, so walk up from the working directory until src/main/java is found.
     */
    private static Path findSrcRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("src").resolve("main").resolve("java");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return Path.of("src", "main", "java");
    }
}
