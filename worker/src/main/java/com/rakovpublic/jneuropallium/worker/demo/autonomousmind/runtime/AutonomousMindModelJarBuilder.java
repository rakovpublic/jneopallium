package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class AutonomousMindModelJarBuilder {
    private AutonomousMindModelJarBuilder() {
    }

    public static void buildModelJar(Path modelJar) throws IOException, URISyntaxException {
        CodeSource codeSource = AutonomousMindModelJarBuilder.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Cannot resolve worker code source for AutonomousMind model jar");
        }
        Path source = Path.of(codeSource.getLocation().toURI());
        Files.createDirectories(modelJar.getParent());
        if (Files.isRegularFile(source) && source.toString().endsWith(".jar")) {
            Files.copy(source, modelJar, StandardCopyOption.REPLACE_EXISTING);
        } else if (Files.isDirectory(source)) {
            jarDirectory(source, modelJar);
        } else {
            throw new IllegalStateException("Unsupported worker code source for AutonomousMind model jar: " + source);
        }
    }

    private static void jarDirectory(Path classesDir, Path jarPath) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            try (var stream = Files.walk(classesDir)) {
                for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
                    String entryName = classesDir.relativize(file).toString().replace('\\', '/');
                    jar.putNextEntry(new JarEntry(entryName));
                    try (InputStream input = Files.newInputStream(file)) {
                        input.transferTo(jar);
                    }
                    jar.closeEntry();
                }
            }
        }
    }
}
