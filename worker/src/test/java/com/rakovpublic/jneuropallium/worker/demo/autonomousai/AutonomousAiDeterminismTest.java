package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AutonomousAiDeterminismTest {
    @Test
    void sameSeedProducesIdenticalResultsAndDifferentSeedCanDiffer() throws Exception {
        AutonomousAiRunManifest a = AutonomousAiDemoLauncher.runOne("baseline_foraging",
                Path.of("target", "jneopallium-autonomous-ai-determinism-a"), 777L, 8);
        AutonomousAiRunManifest b = AutonomousAiDemoLauncher.runOne("baseline_foraging",
                Path.of("target", "jneopallium-autonomous-ai-determinism-b"), 777L, 8);
        AutonomousAiRunManifest c = AutonomousAiDemoLauncher.runOne("baseline_foraging",
                Path.of("target", "jneopallium-autonomous-ai-determinism-c"), 778L, 8);

        String first = Files.readString(Path.of(a.resultsPath), StandardCharsets.UTF_8);
        String second = Files.readString(Path.of(b.resultsPath), StandardCharsets.UTF_8);
        String third = Files.readString(Path.of(c.resultsPath), StandardCharsets.UTF_8);

        assertEquals(first, second);
        assertNotEquals(first, third);
    }
}
