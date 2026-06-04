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
    void runsOwnerTaskInspectionThroughEntryLocalMode() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("owner_task_inspection");

        assertEquals("PASS", manifest.status);
        assertEquals("local", manifest.mode);
        assertEquals("com.rakovpublic.jneuropallium.worker.application.Entry", manifest.entrypoint);
        assertEquals(AutonomousMindContext.class.getName(), manifest.contextClass);
        assertEquals(0, manifest.exitCode);
        assertTrue(Files.exists(Path.of(manifest.modelJarPath)));
        assertTrue(Files.exists(Path.of(manifest.contextJsonPath)));
        assertTrue(Files.exists(Path.of(manifest.layerMetadataPath)));
        assertFalse(AutonomousMindDemoTestSupport.results(manifest).isEmpty());
        assertFalse(AutonomousMindDemoTestSupport.trace(manifest, "perception_trace.jsonl").isEmpty());
        assertFalse(AutonomousMindDemoTestSupport.trace(manifest, "safety_trace.jsonl").isEmpty());
    }
}
