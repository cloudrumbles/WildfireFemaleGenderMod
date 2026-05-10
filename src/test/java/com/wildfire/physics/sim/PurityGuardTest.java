package com.wildfire.physics.sim;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the purity contract of {@code com.wildfire.physics.sim}: nothing in this package
 * may import Minecraft, NeoForge, or any other mod-side code. The whole point of the
 * extraction is that the simulator is freestanding plain Java.
 */
class PurityGuardTest {

    private static final Path SIM_SRC = Path.of("src/main/java/com/wildfire/physics/sim");

    @Test
    void simPackageHasNoMinecraftOrModImports() throws IOException {
        assertTrue(Files.isDirectory(SIM_SRC), "sim package source dir not found at " + SIM_SRC);

        try (Stream<Path> files = Files.walk(SIM_SRC)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(PurityGuardTest::checkFile);
        }
    }

    private static void checkFile(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            fail("could not read " + file + ": " + e.getMessage());
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (!line.startsWith("import ")) continue;
            String imp = line.substring("import ".length());
            if (imp.startsWith("static ")) imp = imp.substring("static ".length());
            // strip trailing semicolon
            if (imp.endsWith(";")) imp = imp.substring(0, imp.length() - 1);

            if (imp.startsWith("net.minecraft.") || imp.startsWith("net.neoforged.")) {
                fail(file + ":" + (i + 1) + " imports Minecraft/NeoForge: " + imp);
            }
            if (imp.startsWith("com.wildfire.") && !imp.startsWith("com.wildfire.physics.sim.")) {
                fail(file + ":" + (i + 1) + " imports mod code outside sim: " + imp);
            }
        }
    }
}
