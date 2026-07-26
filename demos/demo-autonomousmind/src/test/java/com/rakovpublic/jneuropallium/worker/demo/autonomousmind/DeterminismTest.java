package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindFullRunLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterminismTest {
    @Test
    void sameSeedProducesIdenticalResultsAndDifferentSeedMayDiffer() throws Exception {
        Path rootA = Path.of("target", "jneopallium-autonomous-mind-determinism-a");
        Path rootB = Path.of("target", "jneopallium-autonomous-mind-determinism-b");
        Path rootC = Path.of("target", "jneopallium-autonomous-mind-determinism-c");

        AutonomousMindManifest first = AutonomousMindFullRunLauncher.runOne("owner_task_inspection", rootA, 12345L, null);
        AutonomousMindManifest second = AutonomousMindFullRunLauncher.runOne("owner_task_inspection", rootB, 12345L, null);
        AutonomousMindManifest third = AutonomousMindFullRunLauncher.runOne("owner_task_inspection", rootC, 54321L, null);

        String firstResults = Files.readString(Path.of(first.resultPaths.get("results.jsonl")));
        String secondResults = Files.readString(Path.of(second.resultPaths.get("results.jsonl")));
        String thirdResults = Files.readString(Path.of(third.resultPaths.get("results.jsonl")));

        assertEquals(firstResults, secondResults);
        assertNotEquals(firstResults, thirdResults);
    }
}
