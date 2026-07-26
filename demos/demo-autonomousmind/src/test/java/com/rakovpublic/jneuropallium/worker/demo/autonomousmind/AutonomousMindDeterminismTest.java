package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindFullRunLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AutonomousMindDeterminismTest {
    @Test
    void sameSeedProducesSameVideoGameResultsAndDifferentSeedMayDiffer() throws Exception {
        AutonomousMindManifest first = AutonomousMindFullRunLauncher.runOneDirectForTest(
                "baseline_foraging", Path.of("target", "jneopallium-autonomous-mind-v1-det-a"), 777L, null);
        AutonomousMindManifest second = AutonomousMindFullRunLauncher.runOneDirectForTest(
                "baseline_foraging", Path.of("target", "jneopallium-autonomous-mind-v1-det-b"), 777L, null);
        AutonomousMindManifest third = AutonomousMindFullRunLauncher.runOneDirectForTest(
                "baseline_foraging", Path.of("target", "jneopallium-autonomous-mind-v1-det-c"), 778L, null);

        String firstRows = Files.readString(Path.of(first.resultPaths.get("results.jsonl")));
        String secondRows = Files.readString(Path.of(second.resultPaths.get("results.jsonl")));
        String thirdRows = Files.readString(Path.of(third.resultPaths.get("results.jsonl")));

        assertEquals(firstRows, secondRows);
        assertNotEquals(firstRows, thirdRows);
    }
}
