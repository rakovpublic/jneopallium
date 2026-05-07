# Jneopallium ↔ Eclipse Milo OPC UA Integration — Implementation Spec

> **Audience:** Claude Code, executing against a working clone of
> `https://github.com/rakovpublic/jneopallium` on a developer workstation
> with JDK 17 and Maven ≥ 3.9 installed.
>
> **Deliverable:** A working OPC UA bridge that lets Jneopallium read industrial
> measurements from any OPC UA server (PLC, SCADA, simulator, digital twin) as
> typed `MeasurementSignal` / `AlarmSignal` / `InterlockSignal` objects, run them
> through the existing safety-gated cognitive-control pipeline, and write
> `SetpointSignal` / `ActuatorCommandSignal` / `TransparencyLogSignal` decisions
> back to the plant — never bypassing the harm gate, always honoring operator
> override, and never executing an actuator write outside the configured
> per-loop `SafetyMode` (`SHADOW` / `ADVISORY` / `AUTONOMOUS`).
>
> **Library:** Eclipse Milo `1.1.1` (current as of Feb 2026 — the draft review
> document referenced `1.1.3`, which does not exist. Use `1.1.1` everywhere.)

---

## 0. Ground rules

These are non-negotiable. Every PR in this work stream must respect them.

1. **No raw write to a field actuator from neuron output.** Every write goes
   through the chain: `PlanningNeuron` → `SafetyGateNeuron` → `SafetyMode`
   check → `OperatorOverrideSignal` check → `OpcUaCommandOutputAggregator`.
   The aggregator itself rejects any `ActuatorCommandSignal` whose
   `execute=false` (shadow) or whose loop is in shadow mode by config.
2. **Interlocks have direct authority.** If `InterlockSignal.tripped == true`,
   the aggregator MUST write the loop to its fail-safe value regardless of
   anything else in the same tick. This is the only permitted bypass.
3. **Operator override wins.** When `OperatorOverrideSignal` is active for a
   tag, the aggregator does not write that tag from any neuron-derived signal
   for the duration of the override. Override applies to *regulatory* control,
   not to interlocks.
4. **Every write produces an audit record.** Each accepted, suppressed, or
   rejected write emits a `TransparencyLogSignal` (or industrial equivalent)
   that is persisted via `OpcUaTransparencyLogOutput` to a configurable OPC UA
   audit node *and* a local file. No silent writes.
5. **Quality propagates.** A `MeasurementSignal` derived from an OPC UA
   `DataValue` whose `StatusCode` is not `good()` is emitted with
   `Quality.UNCERTAIN` or `Quality.BAD` — never `GOOD`. Downstream neurons
   already account for this; do not "clean" quality at the bridge.
6. **Wall-clock timestamps come from OPC UA server.** Use the
   `sourceTimestamp` from the `DataValue`, falling back to `serverTimestamp`,
   falling back to `System.currentTimeMillis()` only if both are null. Never
   silently substitute local time.

If you ever find yourself writing a code path that violates one of these,
stop and surface it.

---

## 1. Current-state audit (verified by reading the repo)

### 1.1 Module layout
```
jneopallium/
├── pom.xml          ← parent (groupId com.rakovpublic.jneopallium, artifactId wrapper, version 1.0)
├── master/          ← Spring Boot WAR, depends on worker
│   └── pom.xml      ← Spring Boot 2.5.12 + 2.7.18 (BOTH EOL), maven-compiler release 11
└── worker/          ← core neuron-net engine + signals + I/O
    └── pom.xml      ← maven-compiler release 11, gRPC 1.75, protobuf 3.25.5 AND 4.34.0 (conflict)
```

### 1.2 What already exists (do NOT recreate)

In `worker/src/main/java/com/rakovpublic/jneuropallium/`:

* **Industrial signals** (`worker.net.signals.impl.industrial`): `MeasurementSignal`,
  `SetpointSignal`, `ActuatorCommandSignal`, `AlarmSignal`, `InterlockSignal`,
  `DegradationSignal`, `EfficiencySignal`, `BatchStateSignal`,
  `OperatorOverrideSignal`, `MaintenanceWindowSignal`. All carry an explicit
  wall-clock `timestamp` field per industrial-compliance spec §3.
* **Industrial neurons** (`worker.net.neuron.impl.industrial`): full set —
  `SensorNeuron`, `MeasurementValidatorNeuron`, `PIDNeuron`, `CascadeNeuron`,
  `FeedForwardNeuron`, `SetpointOptimiserNeuron`, `AlarmAggregationNeuron`,
  `ModeControllerNeuron`, `ProcessModelNeuron`, `DegradationModelNeuron`,
  `ProductQualityModelNeuron`, `MPCPlanningNeuron`, `CampaignPlanningNeuron`,
  `MaintenanceSchedulingNeuron`, `SafetyGateNeuron`, `ActuatorNeuron`,
  `InterlockNeuron`, `OscillationMonitorNeuron`, `EnergyAccountingNeuron`,
  plus their `I*Neuron` interfaces and supporting enums (`AlarmPriority`,
  `BatchPhase`, `OverrideKind`, `PlantMode`, `Quality` (`GOOD/BAD/UNCERTAIN`),
  `SafetyMode` (`SHADOW/ADVISORY/AUTONOMOUS`)).
* **Transparency log signal**: `com.rakovpublic.jneuropallium.ai.signals.fast.TransparencyLogSignal`
  (note this lives in the `ai` package tree, not `industrial` — keep it there;
  it is the cross-cutting audit primitive).
* **Input/output contracts**:
  * `com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput`
    — `readSignals()`, `getName()`, `getDesiredResults()`, `getDefaultProcessingFrequency()`.
  * `com.rakovpublic.jneuropallium.worker.application.IOutputAggregator`
    — `save(List<IResult>, long timestamp, long run, IContext)`.
  * Reference implementations: `KafkaInitInput`, `RedisInitInput`,
    `FileInitInput` (latter is a stub).

### 1.3 Known defects in the current build (fix as part of Phase 1)

* `worker/pom.xml` line 129: `<protocExecutable>C:\Users\dmytr\Downloads\protoc-34.1-win64\bin\protoc.exe</protocExecutable>`
  — hardcoded Windows path. Build is broken on Linux/macOS CI.
* `worker/pom.xml` declares `protobuf-java` twice — `3.25.5` (line 64–67) and
  `4.34.0` (line 92–96). Maven dedup picks last-wins, but the gRPC stack uses
  `3.25.5`-generated stubs, producing classpath confusion.
* `master/pom.xml` declares **two** Spring Boot artifacts at **two different
  versions** (`spring-boot-starter-web` 2.5.12, `spring-boot` 2.7.18). Both
  are end-of-life and ABI-incompatible with Java 17 + jakarta namespace.
* `worker/pom.xml` uses `javax.annotation-api:1.2` — fine on Java 11, replaced
  by `jakarta.annotation-api` on Java 17 / Spring Boot 3.

---

## 2. Phasing

The work splits into four phases. Phases 1 and 2 are mandatory; Phase 3 is the
demo deliverable; Phase 4 is optional consolidation.

| Phase | Goal | Branch name |
|---|---|---|
| **1** | Migrate build + code from Java 11 to Java 17, clean POMs, kill the Windows path bug, move master to Spring Boot 3, all existing tests still green | `chore/java17-upgrade` |
| **2** | Implement the OPC UA bridge module (`worker/.../opcua`) per §4 | `feat/opcua-bridge` |
| **3** | End-to-end demo against Milo's public demo server + an embedded test server in CI | `demo/opcua-e2e` |
| **4** *(optional)* | Extract the bridge into a separate `gateway` Maven module so it can be deployed and audited independently of the neuron-net core | `refactor/opcua-gateway-module` |

Phase 1 must merge before Phase 2 starts — Milo 1.1.1 requires JDK 17 to compile.

---

## 3. Phase 1 — Java 11 → 17 migration

### 3.1 Goal
Build from a clean `~/.m2` on JDK 17 with `mvn -B clean verify` succeeding for
both modules, all existing JUnit 5 tests green, no new compiler warnings on
`-Xlint:all`.

### 3.2 Step-by-step

#### 3.2.1 Parent POM (`pom.xml` at repo root)

The current parent is essentially empty — promote it into a real reactor POM
that owns the toolchain, plugin versions, and dependency BOM imports.

Replace the parent `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.rakovpublic.jneopallium</groupId>
    <artifactId>wrapper</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>master</module>
        <module>worker</module>
    </modules>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- Library versions (single source of truth) -->
        <milo.version>1.1.1</milo.version>
        <spring-boot.version>3.3.5</spring-boot.version>
        <jackson.version>2.17.2</jackson.version>
        <log4j.version>2.25.4</log4j.version>
        <gson.version>2.11.0</gson.version>
        <jedis.version>5.2.0</jedis.version>
        <kafka.version>3.9.2</kafka.version>
        <protobuf.version>3.25.5</protobuf.version>
        <grpc.version>1.75.0</grpc.version>
        <junit.version>5.10.2</junit.version>
        <mockito.version>5.11.0</mockito.version>
        <slf4j.version>2.0.16</slf4j.version>
        <jakarta-annotation.version>2.1.1</jakarta-annotation.version>

        <!-- Plugin versions -->
        <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
        <maven-surefire-plugin.version>3.5.0</maven-surefire-plugin.version>
        <maven-failsafe-plugin.version>3.5.0</maven-failsafe-plugin.version>
        <maven-war-plugin.version>3.4.0</maven-war-plugin.version>
        <protobuf-maven-plugin.version>0.6.1</protobuf-maven-plugin.version>
        <os-maven-plugin.version>1.7.1</os-maven-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Eclipse Milo BOM keeps client/server/stack at one consistent version -->
            <dependency>
                <groupId>org.eclipse.milo</groupId>
                <artifactId>milo-bom</artifactId>
                <version>${milo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Boot 3 BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- JUnit BOM -->
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <release>${maven.compiler.release}</release>
                        <compilerArgs>
                            <arg>-Xlint:all,-serial,-processing</arg>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                    <version>${maven-failsafe-plugin.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

Why these changes are needed: the existing parent doesn't manage *anything* —
versions are repeated and drift between modules. Centralizing here means
Phase 2's `opcua` work touches one version property when Milo bumps.

#### 3.2.2 `worker/pom.xml`

Apply the following transformations:

1. **Remove the duplicate `protobuf-java` dependency** (the `4.34.0` block).
   Keep only the `${protobuf.version}` (3.25.5) one.
2. **Remove the hardcoded `<protocExecutable>` line.** The
   `<protocArtifact>` line below it already resolves the right protoc binary
   for the host OS via `${os.detected.classifier}`. The `protocExecutable`
   override only existed to work around a local install on the original
   author's Windows machine — it is not portable.
3. **Replace `javax.annotation-api 1.2` with `jakarta.annotation-api`**
   (managed in parent at `2.1.1`). All current usages in worker
   (`@Generated`, `@Nullable` etc.) have direct jakarta equivalents.
4. **Inherit versions from parent** for jackson, log4j, gson, jedis, kafka,
   junit, mockito, gRPC.
5. **Bump `maven-compiler-plugin` to `3.13.0`** (managed in parent).
6. **Bump `maven-surefire-plugin` to `3.5.0`** (managed in parent).

The cleaned `worker/pom.xml` should look like:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.rakovpublic.jneopallium</groupId>
        <artifactId>wrapper</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>worker</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- Core JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>${log4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j2-impl</artifactId>
            <version>${log4j.version}</version>
        </dependency>

        <!-- Annotations (jakarta replaces javax on Java 17+) -->
        <dependency>
            <groupId>jakarta.annotation</groupId>
            <artifactId>jakarta.annotation-api</artifactId>
            <version>${jakarta-annotation.version}</version>
        </dependency>

        <!-- Storage / messaging -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>${jedis.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>${kafka.version}</version>
        </dependency>

        <!-- gRPC + protobuf (single, consistent protobuf version) -->
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>${protobuf.version}</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty-shaded</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
            <version>${grpc.version}</version>
        </dependency>

        <!-- ============== OPC UA (Phase 2) ============== -->
        <!-- Versions come from the milo-bom imported in the parent. -->
        <dependency>
            <groupId>org.eclipse.milo</groupId>
            <artifactId>milo-sdk-client</artifactId>
        </dependency>
        <!-- Server SDK is included so the test harness in Phase 3 can spin up
             an embedded OPC UA server. Scope is `test` to keep it out of the
             worker runtime jar. -->
        <dependency>
            <groupId>org.eclipse.milo</groupId>
            <artifactId>milo-sdk-server</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Tests -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>${os-maven-plugin.version}</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>${protobuf-maven-plugin.version}</version>
                <configuration>
                    <!-- Cross-platform: protoc is downloaded for the host OS -->
                    <protocArtifact>
                        com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}
                    </protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>
                        io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}
                    </pluginArtifact>
                    <clearOutputDirectory>false</clearOutputDirectory>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Note one subtle change: the protoc artifact version is now bound to
`${protobuf.version}` (3.25.5), not 3.25.8 as in the original. Keeping protoc
and protobuf-java in lockstep removes a class of "generated stubs reference a
constant that doesn't exist at runtime" bugs.

#### 3.2.3 `master/pom.xml` — Spring Boot 2 → 3 migration

This is the riskiest change in Phase 1 because Spring Boot 3:
* Requires Java 17 (we're moving anyway).
* Renames `javax.*` → `jakarta.*` throughout (`javax.servlet.*`,
  `javax.persistence.*`, `javax.validation.*`, `javax.annotation.*`).
* Drops a number of deprecated APIs (`WebSecurityConfigurerAdapter` etc.).

Replacement `master/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.rakovpublic.jneopallium</groupId>
        <artifactId>wrapper</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>master</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    <name>jneuronnet.master</name>
    <url>https://github.com/rakovpublic/jneopallium</url>

    <dependencies>
        <dependency>
            <groupId>com.rakovpublic.jneopallium</groupId>
            <artifactId>worker</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <!-- Spring Boot 3 (jakarta namespace, requires Java 17) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- War packaging needs the servlet container in `provided` scope -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>${log4j.version}</version>
        </dependency>

        <!-- Tests -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>jneuronnetmaster</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
            <plugin>
                <artifactId>maven-war-plugin</artifactId>
                <version>${maven-war-plugin.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3.2.4 Source-level migration in `master/`

Run `mvn -pl master -am clean compile` first to surface the broken imports,
then for each compile error:

* `import javax.servlet.*;` → `import jakarta.servlet.*;`
* `import javax.persistence.*;` → `import jakarta.persistence.*;`
* `import javax.validation.*;` → `import jakarta.validation.*;`
* `import javax.annotation.PostConstruct;` → `import jakarta.annotation.PostConstruct;`
* `extends WebSecurityConfigurerAdapter` → migrate to a `SecurityFilterChain`
  bean (the standard Spring Security 6 pattern). If `master/` has no security
  config today, this may not apply — verify.

Run the [OpenRewrite Spring Boot 3 migration recipe] as a one-shot:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:5.41.0:run \
  -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_3 \
  -Drewrite.exportDatatables=true
```

Then commit and review the diff. Spot-check rather than blindly trust.

#### 3.2.5 Source-level migration in `worker/`

The worker is plain Java, no Jakarta EE — the migration risk is low, but
audit for:

* `Class.newInstance()` (deprecated since 9, removed in some tooling)
  → `clazz.getDeclaredConstructor().newInstance()`. Used in `KafkaInitInput`
  and others as part of polymorphic signal deserialization.
* `new JsonParser().parse(...)` in `KafkaInitInput` — Gson 2.11 deprecated
  the instance method. Replace with the static `JsonParser.parseString(...)`.
* `Integer.parseInt` paths that may now be non-null — the new compiler is
  stricter on `null` in autoboxing contexts.
* No usage of `sun.*` or `com.sun.*` is permitted; if any sneaks in, replace.

Run the upgrade as:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:5.41.0:run \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava17
```

#### 3.2.6 Verification gate

Phase 1 is complete only when **all** of the following commands succeed
on a clean checkout with `JAVA_HOME` pointing to a JDK 17 install:

```bash
mvn -B -U clean verify
mvn -B dependency:tree -Dverbose | grep -E "(WARN|conflict)" || true   # no conflicts
mvn -B versions:display-dependency-updates                             # informational
javac --version  # must report 17.x
```

Existing test suites (`TestPlanPhase1.md`, `TestPlanPhase2.md`) must remain
green. Do not edit a test to make it pass; if a test legitimately breaks
because of the migration (e.g. a Spring 6 behaviour change), surface it as a
separate commit with a written justification.

---

## 4. Phase 2 — OPC UA bridge implementation

### 4.1 Module layout

The bridge lives entirely inside `worker/`. We do **not** create a new Maven
module yet (that's Phase 4). The new packages are:

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/
├── net/neuron/impl/industrial/opcua/   ← bridge wiring + config (no neurons here yet)
│   ├── OpcUaBridgeConfig.java
│   ├── OpcUaNodeBinding.java
│   ├── OpcUaSignalMapper.java
│   ├── MiloOpcUaClientService.java
│   └── package-info.java
├── net/signals/industrial/opcua/       ← reserved; keep empty for now
│   └── package-info.java               (existing industrial signals are reused as-is)
├── input/opcua/                        ← IInitInput implementations
│   ├── OpcUaMeasurementInput.java
│   ├── OpcUaAlarmInput.java
│   └── package-info.java
└── output/opcua/                       ← IOutputAggregator implementations
    ├── OpcUaCommandOutputAggregator.java
    ├── OpcUaTransparencyLogOutput.java
    └── package-info.java
```

Tests:
```
worker/src/test/java/com/rakovpublic/jneuropallium/worker/.../opcua/
worker/src/test/resources/opcua/        ← certs, server config for embedded test server
```

The package names match the article's recommendation, with one correction
already in the source tree: the root package is `com.rakovpublic.jneuropallium`
(note the **u** between `jne` and `ropallium`), not `jneuropallium`. Use what
the existing tree uses.

### 4.2 Maven coordinates and dependency scope

The Milo client SDK was already declared in `worker/pom.xml` in §3.2.2.
Confirm with:

```bash
mvn -pl worker dependency:tree -Dincludes=org.eclipse.milo
```

Expected output includes:
```
[INFO] +- org.eclipse.milo:milo-sdk-client:jar:1.1.1:compile
[INFO] |  +- org.eclipse.milo:milo-stack-client:jar:1.1.1:compile
[INFO] |  +- org.eclipse.milo:milo-stack-core:jar:1.1.1:compile
[INFO] |  +- io.netty:netty-handler:jar:4.1.x:compile
[INFO] |  ...
```

If you get `1.1.3`, the BOM didn't load — check parent POM has the
`milo-bom` import block.

### 4.3 Configuration model

The bridge must be configured declaratively, not programmatically — operators
audit YAML, not Java. Use a Java `record` (JDK 17 feature) for immutability.

#### `OpcUaBridgeConfig.java`

```java
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level configuration for an OPC UA bridge instance.
 *
 * Loaded from YAML at startup; immutable. Hot-reloading deliberately
 * unsupported — config changes require a controlled restart of the bridge,
 * which is the expected industrial workflow (MoC — Management of Change).
 */
public record OpcUaBridgeConfig(
        ConnectionConfig connection,
        SecurityConfig   security,
        List<NodeBindingConfig> reads,
        List<NodeBindingConfig> writes,
        List<NodeBindingConfig> alarms,
        AuditConfig audit,
        Map<String, SafetyMode> perLoopSafetyMode,
        Duration tickInterval
) {
    public OpcUaBridgeConfig {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(security,   "security");
        reads             = reads             == null ? List.of() : List.copyOf(reads);
        writes            = writes            == null ? List.of() : List.copyOf(writes);
        alarms            = alarms            == null ? List.of() : List.copyOf(alarms);
        perLoopSafetyMode = perLoopSafetyMode == null ? Map.of()  : Map.copyOf(perLoopSafetyMode);
        tickInterval      = tickInterval      == null ? Duration.ofMillis(250) : tickInterval;
    }

    public record ConnectionConfig(
            String endpointUrl,        // e.g. opc.tcp://plant.local:4840
            String applicationName,    // e.g. "Jneopallium-OPCUA-Bridge"
            String applicationUri,     // urn:rakovpublic:jneopallium:bridge:<host>
            Duration requestTimeout,
            Duration sessionTimeout,
            int     keepAliveFailuresAllowed
    ) {}

    public record SecurityConfig(
            SecurityPolicy policy,
            MessageSecurityMode mode,
            String  pkiDir,           // dir holding the bridge's keystore + trust list
            String  certAlias,        // alias inside keystore
            String  certPassword,     // (load from env / vault, not the YAML file)
            Authentication auth
    ) {
        public enum SecurityPolicy { NONE, BASIC256SHA256, AES128_SHA256_RSAOAEP, AES256_SHA256_RSAPSS }
        public enum MessageSecurityMode { NONE, SIGN, SIGN_AND_ENCRYPT }
        public sealed interface Authentication permits Anonymous, UsernamePassword, X509 {}
        public record Anonymous() implements Authentication {}
        public record UsernamePassword(String username, String passwordEnv) implements Authentication {}
        public record X509(String certAlias) implements Authentication {}
    }

    public record NodeBindingConfig(
            String  loopId,           // logical loop name, e.g. "FIC-101"
            String  nodeId,           // OPC UA NodeId in compact form: "ns=2;s=Plant.FIC101.PV"
            String  signalTag,        // ISA-95 tag emitted on Jneopallium signals
            Direction direction,      // READ | WRITE | BOTH
            Double  failSafeValue,    // value to write on interlock; null if not applicable
            Double  rampRateMaxPerSec,// safety: max change/sec on writes; null = no limit
            Double  minClampValue,
            Double  maxClampValue
    ) {
        public enum Direction { READ, WRITE, BOTH }
    }

    public record AuditConfig(
            String localAuditFile,    // append-only JSONL; rotated externally
            String opcUaAuditNodeId,  // optional: OPC UA String node to also write to
            boolean writeRejectedToAudit
    ) {}
}
```

#### YAML loader

Use Jackson's YAML module (already an indirect dep via Spring Boot, but add
explicitly to the worker so the bridge has no Spring dep):

Add to `worker/pom.xml`:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>${jackson.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>${jackson.version}</version>
</dependency>
```

Then a tiny loader:

```java
public final class OpcUaBridgeConfigLoader {
    public static OpcUaBridgeConfig load(Path yaml) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper.readValue(yaml.toFile(), OpcUaBridgeConfig.class);
    }
    private OpcUaBridgeConfigLoader() {}
}
```

`FAIL_ON_UNKNOWN_PROPERTIES = true` is deliberate — silently ignoring a
typo'd `endpoindUrl` in the YAML would be a safety incident waiting to happen.

#### Example YAML

```yaml
connection:
  endpointUrl: "opc.tcp://milo.digitalpetri.com:62541/milo"
  applicationName: "Jneopallium-OPCUA-Bridge"
  applicationUri: "urn:rakovpublic:jneopallium:bridge:dev01"
  requestTimeout: "PT5S"
  sessionTimeout: "PT2M"
  keepAliveFailuresAllowed: 3

security:
  policy: BASIC256SHA256
  mode: SIGN_AND_ENCRYPT
  pkiDir: "/var/lib/jneopallium/opcua-pki"
  certAlias: "bridge-cert"
  certPassword: "${OPCUA_CERT_PASSWORD}"
  auth:
    type: "UsernamePassword"
    username: "User"
    passwordEnv: "OPCUA_USER_PASSWORD"

reads:
  - loopId: "TIC-101"
    nodeId: "ns=2;s=Plant.Temperature"
    signalTag: "PLANT.TIC101.PV"
    direction: READ

writes:
  - loopId: "FIC-101"
    nodeId: "ns=2;s=Plant.TargetMotorSpeed"
    signalTag: "PLANT.FIC101.SP"
    direction: WRITE
    failSafeValue: 0.0
    rampRateMaxPerSec: 5.0
    minClampValue: 0.0
    maxClampValue: 100.0

alarms:
  - loopId: "PLANT-ALARMS"
    nodeId: "ns=2;s=Plant.Alarm"
    signalTag: "PLANT.ALARM"
    direction: READ

audit:
  localAuditFile: "/var/log/jneopallium/opcua-audit.jsonl"
  opcUaAuditNodeId: "ns=2;s=Plant.JneopalliumAuditLog"
  writeRejectedToAudit: true

perLoopSafetyMode:
  TIC-101:  SHADOW
  FIC-101:  ADVISORY

tickInterval: "PT0.25S"
```

### 4.4 `OpcUaNodeBinding` — internal binding

The config record is loaded once and projected into a runtime structure that
holds the parsed `NodeId`, the `MonitoredItem` reference (for reads), and
the latest cached `DataValue` (for diff suppression on writes):

```java
public final class OpcUaNodeBinding {
    public final String loopId;
    public final String signalTag;
    public final NodeId nodeId;
    public final OpcUaBridgeConfig.NodeBindingConfig config;

    private volatile DataValue lastSeen;       // for read-side
    private volatile double    lastWritten;    // for write-side rate limiting
    private volatile long      lastWrittenAt;  // epoch millis

    public OpcUaNodeBinding(OpcUaBridgeConfig.NodeBindingConfig cfg) {
        this.loopId    = cfg.loopId();
        this.signalTag = cfg.signalTag();
        this.nodeId    = NodeId.parse(cfg.nodeId());
        this.config    = cfg;
    }
    // getters / atomic update methods
}
```

`NodeId.parse(...)` is `org.eclipse.milo.opcua.stack.core.types.builtin.NodeId.parse(String)`.

### 4.5 `OpcUaSignalMapper` — OPC UA ↔ Jneopallium signal translation

Pure functions, no I/O — easy to unit-test exhaustively. This is where the
quality propagation rule from §0.5 lives.

```java
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public final class OpcUaSignalMapper {

    public static MeasurementSignal toMeasurement(OpcUaNodeBinding b, DataValue dv) {
        double value = coerceDouble(dv.value());
        Quality q = mapQuality(dv.statusCode());
        long ts = pickTimestamp(dv);
        return new MeasurementSignal(b.signalTag, value, q, ts);
    }

    public static AlarmSignal toAlarm(OpcUaNodeBinding b, DataValue dv) {
        // Convention: the alarm node carries an Int32 priority code 0-1000
        // mapped per ISA-18.2 to a four-tier priority. Adjust if your plant
        // uses a different mapping.
        int code = (Integer) dv.value().value();
        AlarmPriority pri =
                  code >= 700 ? AlarmPriority.CRITICAL
                : code >= 400 ? AlarmPriority.HIGH
                : code >= 100 ? AlarmPriority.LOW
                              : AlarmPriority.JOURNAL;
        long ts = pickTimestamp(dv);
        return new AlarmSignal(pri, b.signalTag, "OPCUA-" + code, ts);
    }

    public static DataValue toDataValue(SetpointSignal s) {
        return DataValue.valueOnly(Variant.ofDouble(s.getSetpoint()));
    }
    public static DataValue toDataValue(ActuatorCommandSignal s) {
        return DataValue.valueOnly(Variant.ofDouble(s.getTargetValue()));
    }

    /* ==================== helpers ==================== */

    private static Quality mapQuality(StatusCode sc) {
        if (sc == null) return Quality.UNCERTAIN;
        if (sc.isGood())    return Quality.GOOD;
        if (sc.isBad())     return Quality.BAD;
        return Quality.UNCERTAIN;
    }

    private static long pickTimestamp(DataValue dv) {
        if (dv.sourceTime() != null) return dv.sourceTime().getJavaTime();
        if (dv.serverTime() != null) return dv.serverTime().getJavaTime();
        return System.currentTimeMillis();
    }

    private static double coerceDouble(Variant v) {
        Object o = v.value();
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b ? 1.0 : 0.0;
        throw new IllegalStateException("Cannot coerce OPC UA value to double: " + o.getClass());
    }

    private OpcUaSignalMapper() {}
}
```

Verify against the actual Milo 1.1.1 API names — in particular,
`DataValue.sourceTime()` vs `DataValue.getSourceTime()` may vary by point
release. The Milo `BrowseExample` and `ReadExample` in `milo-examples` are
the source of truth.

### 4.6 `MiloOpcUaClientService` — connection lifecycle

This service owns the `OpcUaClient`, the single `OpcUaSubscription`, and
the reconnect loop. It is not a Jneopallium component itself — it is a
shared dependency of all the inputs and the output aggregator.

Responsibilities:
* Build the `OpcUaClient` from `ConnectionConfig` + `SecurityConfig`.
* Connect; create one subscription with a publishing interval matching
  `tickInterval`.
* Add monitored items for every `READ`/`BOTH` binding from `reads` + `alarms`.
* Maintain a `ConcurrentMap<String /*signalTag*/, DataValue>` of latest values.
* Provide a synchronous `writeValues(List<NodeId>, List<DataValue>)` that
  blocks at most `requestTimeout`.
* Reconnect with exponential backoff on disconnect, capped at 30 seconds.
* On reconnect, **do not** silently replay buffered writes — the plant state
  may have changed during the outage. Drop the buffer and emit an
  `AlarmSignal(priority=HIGH, conditionCode="BRIDGE_RECONNECTED")`.

Skeleton:

```java
public final class MiloOpcUaClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MiloOpcUaClientService.class);

    private final OpcUaBridgeConfig cfg;
    private final OpcUaClient        client;
    private final OpcUaSubscription  subscription;
    private final Map<NodeId, OpcUaNodeBinding> bindingsByNodeId;
    private final ConcurrentMap<String /*signalTag*/, DataValue> latest = new ConcurrentHashMap<>();
    private final BlockingQueue<DataValue> alarmQueue = new LinkedBlockingQueue<>(10_000);

    public MiloOpcUaClientService(OpcUaBridgeConfig cfg) throws UaException {
        this.cfg = cfg;
        this.client = buildClient(cfg);
        this.client.connect();
        this.subscription = new OpcUaSubscription(client);
        this.bindingsByNodeId = new ConcurrentHashMap<>();
        installListener();
        subscription.create();
        registerBindings();
    }

    public DataValue latest(String signalTag) { return latest.get(signalTag); }

    public void writeAsync(NodeId id, DataValue dv) { /* see §4.8 */ }

    public List<DataValue> drainAlarms() {
        List<DataValue> out = new ArrayList<>();
        alarmQueue.drainTo(out);
        return out;
    }

    @Override public void close() { /* delete subscription, disconnect client */ }

    private static OpcUaClient buildClient(OpcUaBridgeConfig cfg) throws UaException {
        // Use OpcUaClientConfig + endpoint discovery + KeyStoreLoader.
        // Reference: Milo's ClientExampleRunner under
        // milo-examples/client-examples for the canonical pattern.
        // Critical bits:
        //   - DiscoveryClient.getEndpoints(endpointUrl).get()
        //   - filter by securityPolicy + messageSecurityMode
        //   - .setApplicationName(LocalizedText.english(cfg.connection().applicationName()))
        //   - .setApplicationUri(cfg.connection().applicationUri())
        //   - .setRequestTimeout(uint(cfg.connection().requestTimeout().toMillis()))
        //   - .setKeyPair(...) and .setCertificate(...) from the PKI dir
        //   - .setIdentityProvider(buildIdentityProvider(cfg.security().auth()))
        throw new UnsupportedOperationException("TODO: implement per Milo example");
    }

    private void installListener() {
        subscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
            @Override public void onDataReceived(OpcUaSubscription sub,
                                                 List<OpcUaMonitoredItem> items,
                                                 List<DataValue> values) {
                for (int i = 0; i < items.size(); i++) {
                    OpcUaMonitoredItem item = items.get(i);
                    DataValue dv = values.get(i);
                    NodeId nid = item.getReadValueId().getNodeId();
                    OpcUaNodeBinding b = bindingsByNodeId.get(nid);
                    if (b == null) continue;
                    latest.put(b.signalTag, dv);
                    if (isAlarmBinding(b)) {
                        if (!alarmQueue.offer(dv)) {
                            log.warn("Alarm queue full — dropping alarm for {}", b.signalTag);
                        }
                    }
                }
            }
        });
    }

    private void registerBindings() {
        Stream.concat(cfg.reads().stream(), cfg.alarms().stream())
              .forEach(c -> {
                  OpcUaNodeBinding b = new OpcUaNodeBinding(c);
                  bindingsByNodeId.put(b.nodeId, b);
                  OpcUaMonitoredItem mi = OpcUaMonitoredItem.newDataItem(b.nodeId);
                  subscription.addMonitoredItem(mi);
              });
        try {
            subscription.synchronizeMonitoredItems();
        } catch (MonitoredItemSynchronizationException e) {
            e.getCreateResults().forEach(r ->
                log.error("Failed to create MonitoredItem nodeId={} svc={} op={}",
                    r.monitoredItem().getReadValueId().getNodeId(),
                    r.serviceResult(), r.operationResult()));
            throw new IllegalStateException("Could not subscribe to all bindings", e);
        }
    }

    private boolean isAlarmBinding(OpcUaNodeBinding b) {
        return cfg.alarms().contains(b.config);
    }
}
```

Refer to `milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/SubscriptionDataExample.java`
for the canonical subscription pattern (cloned at `/home/claude/milo` during
research; equivalent file is at the same path in any Milo checkout).

### 4.7 `OpcUaMeasurementInput` and `OpcUaAlarmInput` — `IInitInput`

These are thin: the heavy lifting is in `MiloOpcUaClientService`. They
implement `IInitInput.readSignals()` by snapshotting `latest` and translating
via `OpcUaSignalMapper`.

```java
public final class OpcUaMeasurementInput implements IInitInput {
    private final String name;
    private final ProcessingFrequency freq;
    private final MiloOpcUaClientService svc;
    private final List<OpcUaNodeBinding> measurementBindings;

    public OpcUaMeasurementInput(String name,
                                 ProcessingFrequency freq,
                                 MiloOpcUaClientService svc,
                                 List<OpcUaNodeBinding> measurementBindings) {
        this.name = Objects.requireNonNull(name);
        this.freq = freq != null ? freq : MeasurementSignal.PROCESSING_FREQUENCY;
        this.svc  = Objects.requireNonNull(svc);
        this.measurementBindings = List.copyOf(measurementBindings);
    }

    @Override public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>(measurementBindings.size());
        for (OpcUaNodeBinding b : measurementBindings) {
            DataValue dv = svc.latest(b.signalTag);
            if (dv == null) continue;       // not yet observed
            MeasurementSignal s = OpcUaSignalMapper.toMeasurement(b, dv);
            s.setInputName(name);
            out.add(s);
        }
        return out;
    }

    @Override public String getName() { return name; }
    @Override public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }
    @Override public ProcessingFrequency getDefaultProcessingFrequency() { return freq; }
}
```

`OpcUaAlarmInput` follows the same shape but pulls from `svc.drainAlarms()`
and emits `AlarmSignal` instances.

### 4.8 `OpcUaCommandOutputAggregator` — the safety-critical class

This is where the §0 ground rules are enforced. Treat it with the suspicion
appropriate to a class that can move physical equipment.

Algorithm, top to bottom:

1. Collect `IResult`s; pull out three groups:
   * `interlockResults` — `IResult` whose `IResultSignal` is `InterlockSignal`
     with `tripped=true`.
   * `overrideResults` — `OperatorOverrideSignal` instances received this tick
     (kept in an internal map by tag, expiring after their declared duration).
   * `commandResults` — `SetpointSignal` and `ActuatorCommandSignal`.
2. **Tripped interlocks first.** For each, write the binding's
   `failSafeValue` regardless of any other signal. Emit `TransparencyLog`
   with `verdict=INTERLOCK_TRIP` and synchronously block until the write ack
   returns or `requestTimeout` expires. If write fails, log at ERROR level,
   emit a high-priority alarm, and do not retry on the same tick.
3. **Operator overrides** are recorded but produce no field write themselves.
4. For each command in `commandResults`:
   1. Resolve the binding by `tag`. If unknown, emit `TransparencyLog(verdict=REJECTED, reason="UNKNOWN_TAG")` and skip.
   2. If the loop is in active operator override → skip + audit.
   3. Read effective `SafetyMode` for the loop. If `SHADOW` → audit only,
      do not write. If `ADVISORY` → write only when the command's signal
      carries `execute=true` AND a separate operator-confirmation flag (out
      of scope: how that flag arrives — for v1, treat ADVISORY as "audit
      only, surface to UI"). If `AUTONOMOUS` → continue.
   4. **Clamp** to `[minClampValue, maxClampValue]` if set.
   5. **Rate-limit**: if `rampRateMaxPerSec` is set and the proposed step
      exceeds `rampRate * dt`, replace with the maximum allowed step in the
      proposed direction; emit an audit entry noting the clamp.
   6. **Diff-suppress**: if the new value equals the last successfully
      written value within `1e-6` AND the timestamp delta is < 5s, skip the
      write to reduce OPC UA bus load. Still audit.
   7. Translate via `OpcUaSignalMapper.toDataValue(...)`, call
      `svc.writeAsync(nodeId, dv)`.
   8. On ack, emit `TransparencyLog(verdict=APPLIED)`. On nack, emit
      `verdict=FAILED` with the StatusCode.

Skeleton:

```java
public final class OpcUaCommandOutputAggregator implements IOutputAggregator {

    private static final Logger log = LoggerFactory.getLogger(OpcUaCommandOutputAggregator.class);

    private final OpcUaBridgeConfig cfg;
    private final MiloOpcUaClientService svc;
    private final Map<String, OpcUaNodeBinding> writeBindings; // by signalTag
    private final OpcUaTransparencyLogOutput audit;
    private final OverrideRegistry overrides = new OverrideRegistry();

    /* ctor + close */

    @Override
    public void save(List<IResult> results, long timestamp, long run, IContext context) {
        // 1. partition
        var partitioned = partition(results);

        // 2. interlocks first (cannot be vetoed)
        for (IResult r : partitioned.interlocks()) {
            executeInterlock((InterlockSignal) r.getResult(), timestamp, run);
        }

        // 3. record overrides
        for (IResult r : partitioned.overrides()) {
            overrides.record((OperatorOverrideSignal) r.getResult(), timestamp);
        }
        overrides.expireOlderThan(timestamp);

        // 4. commands
        for (IResult r : partitioned.commands()) {
            try {
                processCommand(r, timestamp, run);
            } catch (Exception e) {
                audit.recordRejection(r, "EXCEPTION:" + e.getClass().getSimpleName(), timestamp, run);
                log.error("Command processing failed", e);
            }
        }
    }

    private void processCommand(IResult r, long ts, long run) { /* ... per algorithm above ... */ }
    private void executeInterlock(InterlockSignal s, long ts, long run) { /* ... */ }
}
```

`OverrideRegistry` is a small in-memory map keyed by tag; entries TTL out
after the declared override duration.

### 4.9 `OpcUaTransparencyLogOutput` — append-only audit

* Always writes to the local JSONL file specified in `audit.localAuditFile`,
  using a single-writer thread to guarantee ordering.
* If `audit.opcUaAuditNodeId` is set, also writes a JSON-serialized record
  to that OPC UA String node. This is best-effort — a failure to write the
  OPC UA audit node must NEVER prevent the local audit write.
* Schema (one JSON object per line):
  ```json
  {
    "ts": 1740000000000,
    "run": 12345,
    "verdict": "APPLIED" | "REJECTED" | "INTERLOCK_TRIP" | "OVERRIDE_HOLD" | "FAILED",
    "loopId": "FIC-101",
    "tag": "PLANT.FIC101.SP",
    "proposed": 47.3,
    "effective": 45.0,        // post-clamp / rate-limit
    "reason": "RATE_LIMITED",
    "evidenceNeurons": ["Setpoint-12", "MPC-3"],
    "safetyMode": "AUTONOMOUS"
  }
  ```

### 4.10 What you do NOT need to write in Phase 2

* **No new neuron classes.** The pipeline already has `SafetyGateNeuron`,
  `SetpointOptimiserNeuron`, etc. The bridge plugs into existing neurons via
  the `IInitInput` / `IOutputAggregator` boundaries.
* **No changes to core `Neuron` / `Signal` / `Layer` contracts.** If you
  feel the urge to add a method to `ISignal`, stop and surface it.
* **No new industrial signals.** Reuse `MeasurementSignal`, `SetpointSignal`,
  `ActuatorCommandSignal`, `AlarmSignal`, `InterlockSignal`,
  `OperatorOverrideSignal`, `TransparencyLogSignal`.

---

## 5. Phase 3 — End-to-end demo & integration tests

### 5.1 Embedded test server

Use `milo-sdk-server` (already test-scoped in `worker/pom.xml`) to spin up a
real OPC UA server inside the JVM under JUnit. The test fixture should:

1. Start an `OpcUaServer` on a random free port.
2. Register a `Plant` namespace with these nodes:
   * `ns=2;s=Plant.Temperature` (Double, RW, simulator-driven)
   * `ns=2;s=Plant.Pressure` (Double, RW, simulator-driven)
   * `ns=2;s=Plant.MotorSpeed` (Double, RW)
   * `ns=2;s=Plant.TargetMotorSpeed` (Double, RW)
   * `ns=2;s=Plant.Alarm` (Int32, RW, simulator-driven)
   * `ns=2;s=Plant.AdvisoryMessage` (String, RW)
   * `ns=2;s=Plant.JneopalliumAuditLog` (String, RW)
3. Run a tiny simulator thread that:
   * Drives `Temperature` along a sinusoid + slow drift.
   * Drives `Pressure` to oscillate when `Temperature > 80`.
   * Sets `Alarm` to 800 when `Temperature > 95`.
   * Tracks the latest `TargetMotorSpeed` write and decays `Pressure`
     oscillation when the target speed is reduced.

Reference implementation: copy and adapt
`milo-examples/server-examples` from the Milo repo. Strip everything you
don't need; this fixture should be ~250 lines.

### 5.2 Acceptance scenarios

| Scenario | Setup | Expected | Audit assertion |
|---|---|---|---|
| **S1 — Pure read** | Bridge connects, no writes configured | Within 2s, `OpcUaMeasurementInput.readSignals()` returns 3 `MeasurementSignal`s (T, P, MotorSpeed) with `Quality.GOOD` | Connection records but no command audit entries |
| **S2 — Bad quality** | Server returns `Bad_NoCommunication` for Temperature | `MeasurementSignal.quality == BAD`, value passed through unchanged | None |
| **S3 — SHADOW mode setpoint** | `FIC-101` configured `SHADOW`. Aggregator receives `SetpointSignal(50.0)` | OPC UA node `Plant.TargetMotorSpeed` unchanged | One audit entry, `verdict=REJECTED`, `reason=SHADOW_MODE` |
| **S4 — AUTONOMOUS apply** | Same loop set to `AUTONOMOUS`. `SetpointSignal(50.0)`, current value 30 | `Plant.TargetMotorSpeed` becomes 50.0 (within ramp limit) within 1s | Audit `verdict=APPLIED`, `effective=50.0` |
| **S5 — Rate-limited** | `rampRateMaxPerSec=5`, current 30, signal 50, dt=0.5s | Written value clamped to 32.5 | Audit `verdict=APPLIED`, `reason=RATE_LIMITED`, `effective=32.5` |
| **S6 — Interlock priority** | Same tick: `SetpointSignal(80.0)` AND `InterlockSignal(tripped=true)` for `FIC-101` (failSafe=0) | `Plant.TargetMotorSpeed` becomes 0.0; the 80 setpoint is dropped | Two audit entries: `INTERLOCK_TRIP` (applied) and `REJECTED reason=INTERLOCK_HOLD` |
| **S7 — Operator override** | Active `OperatorOverrideSignal(tag=FIC-101, kind=MANUAL, durationTicks=20)`. Setpoint signal arrives | No write to field | Audit `verdict=OVERRIDE_HOLD` |
| **S8 — Reconnect** | Kill the server mid-run, wait 3s, restart | Bridge reconnects with backoff. After reconnect, an alarm `BRIDGE_RECONNECTED` is emitted; pre-disconnect commands are NOT replayed | Audit shows reconnection event |
| **S9 — Clamp** | `maxClampValue=100`, `SetpointSignal(150.0)` | Written value 100 | Audit `effective=100`, `reason=CLAMPED_HIGH` |
| **S10 — Unknown tag** | `SetpointSignal(tag="NOT_CONFIGURED")` | Skipped | Audit `verdict=REJECTED`, `reason=UNKNOWN_TAG` |
| **S11 — Audit failure isolation** | Make the local audit file unwritable, attempt apply | Apply still succeeds at OPC UA layer | Stderr warning; bridge continues but health check reports degraded |
| **S12 — Public Milo demo** | Configure against `opc.tcp://milo.digitalpetri.com:62541/milo` with `User`/`password` | Bridge connects, lists endpoints, subscribes to `Server_ServerStatus_CurrentTime` | This is a *manual* smoke test, not in CI |

S1-S11 must be JUnit 5 tests in `worker/src/test/java/.../opcua/`. S12 is
documented in a `docs/opcua-bridge-demo.md` and run by hand before any
release.

### 5.3 CI

Add a GitHub Actions job to `.github/workflows/maven-verify.yml` (create if
missing):

```yaml
name: maven-verify
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin', cache: maven }
      - run: mvn -B -U clean verify
```

The integration tests (S1-S11) bind via `maven-failsafe-plugin` as
`*IT.java`; surefire runs the unit tests; failsafe runs the IT tests.

---

## 6. Phase 4 (optional) — Extract the bridge as its own module

After Phase 3 is green, the bridge code can be lifted into a sibling Maven
module — call it `gateway` — that depends on `worker` for the signal types
but is built and deployed separately. The advantage matches the article:
the OPC UA-facing surface can be audited, certified, and permission-limited
on its own. Defer this until you actually need it; it's a structural move
that adds packaging cost without changing behaviour.

---

## 7. Risk register

| # | Risk | Likelihood | Mitigation |
|---|---|---|---|
| R1 | Spring Boot 3 migration breaks `master` controllers because of jakarta namespace | High | Run OpenRewrite first, then surface any handwritten `javax.*` imports. Hold this part separately if needed and ship Java 17 worker first. |
| R2 | Milo 1.1.1 API drift from examples (e.g. `DataValue.sourceTime()` vs `getSourceTime()`) | Medium | Build a smoke test against the Milo public demo server *first*, before writing the full bridge. Fix the API names then. |
| R3 | Hardcoded protoc path replaced with `${os.detected.classifier}` fails on Apple Silicon | Low | `os-maven-plugin` 1.7.1 supports `osx-aarch_64`. Verify with `mvn -X validate \| grep classifier`. |
| R4 | gRPC 1.75 + Java 17 emits illegal-reflective-access warnings from Netty | Low | Add `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED` to surefire `argLine` if observed. |
| R5 | OPC UA security: production deployments often use self-signed certs that the bridge must trust | High in production | Ship a `pkiDir/trusted/certs/` directory convention; document the operator workflow for adding a server cert. Don't auto-trust. |
| R6 | Audit file becomes the single point of failure (disk full → bridge halt) | Medium | Use a circular `RandomAccessFile`-backed log with bounded size, or rely on logrotate + a healthcheck that surfaces "audit degraded" as an `AlarmSignal` rather than halting writes. |
| R7 | `MeasurementSignal.PROCESSING_FREQUENCY` (loop=1, epoch=1) is too aggressive for slow process variables (temperature in a vat) | Low | Allow per-binding override of `ProcessingFrequency` in YAML — defer to v2 if not immediately needed. |
| R8 | Two protobuf versions on classpath at runtime cause `NoSuchFieldError` | Was high pre-cleanup | Phase 1 §3.2.2 removes the duplicate. Verify with `mvn dependency:tree -Dincludes=com.google.protobuf`. |

---

## 8. Acceptance criteria & Definition of Done

### Phase 1
- [ ] `JAVA_HOME` is JDK 17. `mvn -B -U clean verify` is green.
- [ ] No `javax.*` imports remain (`grep -r "javax\\." worker/src master/src` empty bar `javax.crypto` if used).
- [ ] No hardcoded paths in any POM (`grep -r "C:\\\\\\|/Users/" pom.xml master/pom.xml worker/pom.xml` empty).
- [ ] `mvn dependency:tree -Dincludes=com.google.protobuf` shows exactly one `protobuf-java` version.
- [ ] All pre-existing tests in `TestPlanPhase1.md` and `TestPlanPhase2.md` still pass.
- [ ] CI workflow added and green on `main` and the PR branch.

### Phase 2
- [ ] New packages exist as in §4.1.
- [ ] `OpcUaBridgeConfig` round-trips through Jackson YAML for the example
      YAML in §4.3.
- [ ] `MiloOpcUaClientService` connects to the public Milo demo server, lists
      endpoints, and subscribes to one node (`Server_ServerStatus_CurrentTime`).
- [ ] `OpcUaMeasurementInput.readSignals()` returns non-empty within 2s of
      bridge start when at least one binding has had a value tick.
- [ ] `OpcUaCommandOutputAggregator.save(...)` honours the §0 ground rules in
      isolation tests (with `MiloOpcUaClientService` mocked).
- [ ] All bridge classes have package-private access where possible; only
      `OpcUaBridgeConfig`, `OpcUaBridgeConfigLoader`,
      `MiloOpcUaClientService`, and the four `IInitInput`/`IOutputAggregator`
      implementations are public.

### Phase 3
- [ ] Scenarios S1-S11 are automated, in `*IT.java`, run under
      `mvn verify`, and pass on a clean checkout.
- [ ] `docs/opcua-bridge-demo.md` exists with the manual S12 procedure.
- [ ] A `docs/opcua-bridge-architecture.md` includes the data-flow diagram
      from the review article and the §0 ground rules verbatim.

---

## 9. References (read in this order if anything is unclear)

1. **Eclipse Milo source examples**
   `milo-examples/client-examples/src/main/java/org/eclipse/milo/examples/client/`
   — specifically `SubscriptionDataExample`, `ReadExample`, `WriteExample`,
   `KeyStoreLoader` and `ClientExampleRunner`. These are the canonical
   patterns; the bridge code should look like a productionized version of
   them.
2. **OPC UA — getting started.**
   `https://opcfoundation.org/unified-architecture-getting-started/` —
   read sections on AddressSpace, NodeId formats, Quality codes, Sessions vs
   SecureChannels.
3. **OPC UA tutorial.**
   `https://plcprogramming.io/blog/opc-ua-tutorial-complete-guide` — practical
   coverage of namespaces, browse paths, security policies, the difference
   between `MonitoredItem` sampling rate vs `Subscription` publishing rate.
4. **Existing industrial use case**: `use-case-industrial-process-control.md`
   in this repo — read §§ 5, 6, 7. The bridge implements the I/O boundary of
   what that document describes architecturally.
5. **Jneopallium internal references** that the bridge plugs into:
   * `worker/.../net/signals/storage/IInitInput.java`
   * `worker/.../application/IOutputAggregator.java`
   * `worker/.../net/signals/storage/kafka/KafkaInitInput.java` — closest
     existing analogue for `OpcUaMeasurementInput`.
   * `worker/.../net/neuron/impl/industrial/SafetyMode.java` — three modes,
     per-loop.
   * `worker/.../net/neuron/impl/industrial/Quality.java` — already aligned
     with OPC UA quality semantics.

---

## 10. Operator-facing summary (for the README)

Once Phase 3 is merged, add this short section to the project README:

> **OPC UA bridge.** Jneopallium can act as a biologically-inspired,
> safety-gated cognitive-control layer for OPC UA industrial systems via
> Eclipse Milo. The bridge subscribes to PLC / SCADA / simulator nodes,
> emits typed `MeasurementSignal` / `AlarmSignal` instances into the neuron
> network, and writes neuron-derived `SetpointSignal` /
> `ActuatorCommandSignal` decisions back to the field — only after passing
> the harm gate, honouring operator override, respecting per-loop SHADOW /
> ADVISORY / AUTONOMOUS mode, and emitting an audit record for every
> proposed action whether applied, clamped, or rejected. See
> `docs/opcua-bridge-architecture.md`.
