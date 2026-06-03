package com.rakovpublic.jneuropallium.worker.demo.fullrun;

import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoCatalog;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullRunDemoSmokeTest {

    @Test
    void runsEveryDemoThroughEntryLocalModeAndWritesArtifacts() throws Exception {
        List<DemoRunManifest> manifests = FullRunDemoTestSupport.manifests();

        assertEquals(DemoCatalog.all().size(), manifests.size());
        assertTrue(Files.exists(FullRunDemoTestSupport.outputRoot().resolve("summary.json")));

        for (DemoRunManifest manifest : manifests) {
            assertEquals("PASS", manifest.status, manifest.demoId);
            assertEquals("local", manifest.mode, manifest.demoId);
            assertEquals(DemoJsonContext.class.getName(), manifest.contextClass);
            assertEquals(0, manifest.exitCode, manifest.demoId);
            assertTrue(manifest.outputRows > 0, manifest.demoId + " should write result rows");
            assertTrue(manifest.behaviorAssertions.getOrDefault("modeLocal", false), manifest.demoId);
            assertTrue(manifest.behaviorAssertions.getOrDefault("aggregatorCalled", false), manifest.demoId);

            assertTrue(Files.exists(Path.of(manifest.modelJarPath)), manifest.demoId + " model jar missing");
            assertTrue(Files.exists(Path.of(manifest.contextJsonPath)), manifest.demoId + " context missing");
            assertTrue(Files.exists(Path.of(manifest.layerMetaPath)), manifest.demoId + " layer metadata missing");
            assertTrue(Files.exists(Path.of(manifest.outputPath)), manifest.demoId + " output missing");
            assertTrue(Files.exists(Path.of(manifest.auditPath)), manifest.demoId + " audit missing");
            assertTrue(Files.exists(Path.of(manifest.entryLogPath)), manifest.demoId + " entry log missing");
            assertFalse(FullRunDemoTestSupport.outputLines(manifest).isEmpty(), manifest.demoId);
        }
    }
}
