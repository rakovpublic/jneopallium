package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoContext;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousAiFullRunSmokeTest {
    @Test
    void runsBaselineThroughEntryLocalMode() throws Exception {
        AutonomousAiRunManifest manifest = AutonomousAiDemoTestSupport.manifest("baseline_foraging");

        assertEquals("PASS", manifest.status);
        assertEquals("local", manifest.mode);
        assertEquals("com.rakovpublic.jneuropallium.worker.application.Entry", manifest.entrypoint);
        assertEquals(AutonomousAiDemoContext.class.getName(), manifest.contextClass);
        assertEquals(0, manifest.exitCode);
        assertTrue(Files.exists(Path.of(manifest.outputDir).resolve("manifest.json")));
        assertTrue(Files.exists(Path.of(manifest.modelJarPath)));
        assertTrue(Files.exists(Path.of(manifest.contextJsonPath)));
        assertTrue(Files.exists(Path.of(manifest.layerMetaPath)));
        assertFalse(AutonomousAiDemoTestSupport.results(manifest).isEmpty());
        assertTrue(manifest.behaviorAssertions.getOrDefault("rewardIncreases", false));
    }
}
