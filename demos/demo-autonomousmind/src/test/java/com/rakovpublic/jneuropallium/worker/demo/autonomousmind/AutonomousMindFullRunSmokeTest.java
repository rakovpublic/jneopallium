package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindContext;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindFullRunSmokeTest {
    @Test
    void runsBaselineForagingThroughEntryLocalMode() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.entryManifest("baseline_foraging");

        assertEquals("PASS", manifest.status);
        assertEquals("local", manifest.mode);
        assertEquals("com.rakovpublic.jneuropallium.worker.application.Entry", manifest.entrypoint);
        assertEquals(AutonomousMindContext.class.getName(), manifest.contextClass);
        assertEquals(0, manifest.exitCode);
        assertTrue(Files.exists(Path.of(manifest.modelJarPath)));
        assertTrue(Files.exists(Path.of(manifest.contextJsonPath)));
        assertTrue(Files.exists(Path.of(manifest.layerMetadataPath)));
        assertFalse(AutonomousMindDemoTestSupport.results(manifest).isEmpty());
        assertFalse(AutonomousMindDemoTestSupport.trace(manifest, "transparency.jsonl").isEmpty());
        assertTrue(Files.exists(Path.of(manifest.resultPaths.get("safety_summary.json"))));
    }
}
