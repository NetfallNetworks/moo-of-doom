package com.github.netfallnetworks.mooofdoom.cow;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CowMorphHandlerTest {

    @Test
    void startMorphAppliesInvisibilityForRequestedDuration() throws IOException {
        Path sourceFile = findSrcRoot().resolve(
                "com/github/netfallnetworks/mooofdoom/cow/CowMorphHandler.java");
        String source = Files.readString(sourceFile);
        int methodStart = source.indexOf("public static void startMorph(");
        assertTrue(methodStart >= 0, "startMorph must exist");
        int bodyStart = source.indexOf('{', methodStart);
        assertTrue(bodyStart >= 0, "startMorph must have a body");

        int depth = 1;
        int bodyEnd = bodyStart + 1;
        while (bodyEnd < source.length() && depth > 0) {
            char c = source.charAt(bodyEnd++);
            if (c == '{') depth++;
            if (c == '}') depth--;
        }
        assertTrue(depth == 0, "startMorph body must be complete");
        String body = source.substring(bodyStart + 1, bodyEnd - 1);
        Pattern effectApplication = Pattern.compile(
                "player\\s*\\.\\s*addEffect\\s*\\(\\s*new\\s+MobEffectInstance\\s*\\("
                        + "\\s*MobEffects\\s*\\.\\s*INVISIBILITY\\s*,\\s*durationTicks\\s*,"
                        + "\\s*0\\s*,\\s*false\\s*,\\s*false\\s*,\\s*true\\s*\\)\\s*\\)\\s*;");
        assertTrue(effectApplication.matcher(body).find(),
                "startMorph must apply invisibility using durationTicks so the morph survives until expiry");
    }

    // The NeoForge unitTest runner may start below the project root.
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
