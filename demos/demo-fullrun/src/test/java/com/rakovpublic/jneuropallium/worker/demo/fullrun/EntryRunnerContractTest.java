package com.rakovpublic.jneuropallium.worker.demo.fullrun;

import com.rakovpublic.jneuropallium.worker.application.Entry;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryRunnerContractTest {

    @Test
    void entryRejectsWrongArgumentCount() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(List.of(
                javaBinary(),
                Entry.class.getName(),
                "local",
                "missing-jar",
                DemoJsonContext.class.getName()
        ));
        processBuilder.environment().put("CLASSPATH", childClasspath());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        assertEquals(0, exitCode);
        assertTrue(output.contains("incorrect amount of arguments"),
                "Entry should log the wrong-argument rejection, got: " + output);
    }

    @Test
    void entryWithFourArgumentsReachesRunnerAndLocalApplication() throws Exception {
        DemoRunManifest manifest = FullRunDemoTestSupport.manifest("demo-03-drone-mavlink-guard");
        String entryLog = Files.readString(Path.of(manifest.entryLogPath), StandardCharsets.UTF_8);

        assertEquals("local", manifest.mode);
        assertEquals(0, manifest.exitCode);
        assertTrue(manifest.behaviorAssertions.getOrDefault("aggregatorCalled", false));
        assertTrue(entryLog.contains(Entry.class.getName()));
        assertTrue(entryLog.contains(DemoJsonContext.class.getName()));
    }

    private static String childClasspath() {
        String surefireClasspath = System.getProperty("surefire.test.class.path");
        if (surefireClasspath != null && !surefireClasspath.isBlank()) {
            return surefireClasspath;
        }
        return System.getProperty("java.class.path");
    }

    private static String javaBinary() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return javaHome.resolve("bin").resolve(executable).toString();
    }
}
