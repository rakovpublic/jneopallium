# CLAUDE.md — AI Assistant Guide for jneopallium

## Project Overview

**jneopallium** is a Java-based neural network simulation framework that models realistic neuron behavior. It supports local, HTTP-cluster, and gRPC-based distributed execution. The project is published research (doi: SR24703042047) and is under active development.

- **License**: BSD 3-Clause (Copyright 2018 Dmytro Rakovskyi)
- **Language**: Java 11+
- **Build System**: Maven (multi-module)
- **Package Root**: `com.rakovpublic.jneuropallium`

---

## Repository Structure

```
jneopallium/
├── pom.xml                   # Parent POM — declares modules
├── master/                   # Spring Boot WAR — orchestration/API layer
│   ├── pom.xml
│   └── src/main/
│       ├── java/             # Controllers, services, config beans
│       └── resources/
│           ├── application.yml
│           └── log4j2.yml
├── worker/                   # Core neuron engine — JAR with dependencies
│   ├── pom.xml
│   └── src/main/
│       ├── java/             # Neuron model, layers, signals, study algorithms
│       ├── proto/            # gRPC service definitions
│       └── resources/
│           ├── config.properties   # Primary runtime configuration
│           └── log4j2.yml
├── doc/                      # Logo, research PDF
├── .github/
│   ├── workflows/codeql.yml  # CodeQL static analysis CI
│   └── ISSUE_TEMPLATE/       # Bug report, feature request templates
├── TestPlanPhase1.md         # Test plan and completion status
├── TestPlanPhase2.md
├── WorkDiary.md              # Development diary/progress log
└── README.md                 # Project overview and architecture
```

---

## Module Overview

### `master` module (Spring Boot WAR)
The orchestration layer — exposes REST APIs and manages worker nodes.

| Component | Package | Purpose |
|-----------|---------|---------|
| Controllers | `...controller` | REST endpoints: nodes, layers, inputs, config, signals, class loading |
| Services | `...service` | Business logic: config, node management, signals, storage |
| Config | `...config` | Spring beans, property holders |
| Entry Point | `Application.java` | `@SpringBootApplication` bootstrap |

**Packaged as**: `jneuronnetmaster.war`

### `worker` module (Executable JAR)
The core neuron execution engine — runs neuron networks directly.

| Package | Purpose |
|---------|---------|
| `application/` | Entry points: `LocalApplication`, `HttpClusterApplication`, `GRPCServerApplication`, `Entry` |
| `net/neuron/` | Core interfaces: `INeuron`, `IDendrites`, `IAxon`, `ISignalProcessor`, `IWeight`, `INeuronRunner` |
| `net/layers/` | Layer management: `ILayer`, `ILayersMeta`, `IResultLayer` |
| `net/signals/` | Signal handling: `ISignal`, `IInputSignal`, `IResultSignal`, `ISignalStorage`, `IInputLoadingStrategy` |
| `net/study/` | Learning: `ILearningAlgo`, `IDirectLearningAlgorithm`, `IObjectLearningAlgo`, `IResultComparingStrategy` |
| `net/storages/` | Storage abstraction: `IStorage` and file/in-memory/Redis/JSON implementations |
| `util/` | Utilities: `NeuronNetStructureGenerator`, `JarClassLoaderService`, `NeuronParser`, `Context` |

**Entry class**: `Entry.java` — accepts 4 command-line arguments at startup.

---

## Technology Stack

| Technology | Version | Use |
|------------|---------|-----|
| Java | 11 | Primary language |
| Spring Boot | 2.5.12 / 2.7.18 | Master module web framework |
| gRPC | 1.75.0 | Distributed worker communication |
| Protocol Buffers | 3.25.5 | gRPC message definitions |
| Kafka | 3.9.1 | Async input signal source |
| Redis (Jedis) | 5.0.2 | Distributed state storage |
| Jackson | 2.12.7.1 | JSON serialization |
| Gson | 2.8.9 | Alternate JSON handling |
| Log4j 2 | 2.25.3 | Logging |
| JUnit | 4.13.1 | Testing |
| Maven | — | Build system |

---

## Build Commands

```bash
# Build entire project (all modules)
mvn clean package

# Build without running tests
mvn clean package -DskipTests

# Build specific module
mvn clean package -pl worker
mvn clean package -pl master

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Install to local Maven cache
mvn clean install
```

**Build outputs:**
- `master/target/jneuronnetmaster.war`
- `worker/target/worker-<version>-jar-with-dependencies.jar`

---

## Running the Application

### Local (single machine)
```bash
java -jar worker/target/worker-*-jar-with-dependencies.jar <arg1> <arg2> <arg3> <arg4>
```
Uses `LocalApplication` — runs the full neuron net in-process.

### HTTP Cluster
Uses `HttpClusterApplication` — distributes work across HTTP-accessible worker nodes managed by the master Spring Boot app.

### gRPC Cluster
Uses `GRPCServerApplication` — exposes a gRPC server defined in `worker/src/main/proto/jneoapalliumservice.proto`.

**Master module** (manages distributed workers):
```bash
java -jar master/target/jneuronnetmaster.war
```

---

## Configuration

### Worker: `worker/src/main/resources/config.properties`
Primary runtime configuration. Key properties:

```properties
# Execution control
configuration.maxRun=2
configuration.infiniteRun=false
neuron.pool.size=2
worker.threads.amount=1

# Input source type: fileSystem | kafka | redis | inMemory
configuration.input.type=fileSystem

# File paths (use forward slashes even on Windows)
configuration.filesystem.class=sample.LocalFileSystem

# Learning/study
configuration.isteacherstudying=false

# Signal loop ratios
configuration.slowfast.ratio=1
configuration.history.slow.runs=1
configuration.history.fast.runs=1
```

### Master: `master/src/main/resources/application.yml`
Spring Boot configuration (currently empty — relies on defaults).

### Logging: `log4j2.yml`
Each module has its own Log4j 2 YAML configuration in `src/main/resources/`.

---

## Code Conventions

### Naming Conventions
- **Interfaces**: `I` prefix — e.g., `INeuron`, `ILayer`, `ISignal`
- **Implementations**: `*Impl` suffix — e.g., `NeuronImpl`, `LayerImpl`
- **Packages**: lowercase, reverse-domain — `com.rakovpublic.jneuropallium.*`
- **Copyright headers**: `(c) 2023. Rakovskyi Dmytro` — include in new files

### Design Patterns in Use
- **Builder**: `StructBuilder`, `LayerBuilder`
- **Strategy**: `IInputLoadingStrategy`, `IInputInitStrategy`
- **Factory**: class loaders, serializers
- **Wrapper**: JSON serialization wrappers

### Code Quality
- No checkstyle or spotbugs currently configured
- CodeQL static analysis runs on push/PR to `master` (see `.github/workflows/codeql.yml`)
- Follow existing formatting style in the file being modified

---

## Storage Backends

The framework supports pluggable storage — configure the implementation class in `config.properties`:

| Backend | Classes | Use Case |
|---------|---------|---------|
| File System | `LocalFileSystem`, `FileLayersMeta` | Default/development |
| In-Memory | `InMemoryLayerMeta`, `InMemorySignalPersistStorage` | Testing/fast runs |
| Redis | `RedisLayersMeta`, `RedisSignalStorage`, `RedisContext` | Distributed state |
| Kafka | `KafkaInitInput` | Async input streams |

---

## gRPC Service

Defined in `worker/src/main/proto/jneoapalliumservice.proto`:

```proto
service MasterService {
  rpc save(Result) returns (Empty);
  rpc saveDiscriminator(ResultDiscriminator) returns (Empty);
  rpc getRun(Empty) returns (SplitInputConfig);
}
```

Protocol Buffer sources are generated by `protobuf-maven-plugin` during build. Generated sources land in `target/generated-sources/`.

---

## Testing

- Framework: **JUnit 4**
- Runner: **Maven Surefire**
- Test status documented in `TestPlanPhase1.md` and `TestPlanPhase2.md`

**Phase completion status** (per `TestPlanPhase1.md`):
- Phases 1–4: 100% complete
- Phase 7: 80–100% complete
- Phases 5, 6, 8, 9: Skipped or minimal (optional distributed features)

When writing tests:
- Place under `src/test/java/` in the relevant module
- Follow JUnit 4 conventions (`@Test`, `@Before`, `@After`)
- Use `InMemoryLayerMeta` and `InMemorySignalPersistStorage` to avoid file system dependencies

---

## CI/CD

**GitHub Actions** (`.github/workflows/codeql.yml`):
- Triggers: push to `master`, PR to `master`, weekly schedule
- Runs CodeQL analysis (Java/Kotlin)
- Does not deploy — analysis only

---

## Development Workflow

1. Create a feature branch from `master`
2. Make changes in the appropriate module (`master/` or `worker/`)
3. Run `mvn clean package` to ensure the build succeeds
4. Run `mvn test` to ensure tests pass
5. Open a PR targeting `master`

**Contributing**: See `CONTRIBUTING.md` — note that significant contributions require agreement with the core developer.

---

## Key Files for AI Assistants

When making changes, these files are most frequently relevant:

| Task | Key Files |
|------|-----------|
| Add a REST endpoint | `master/src/main/java/.../controller/` |
| Add a service | `master/src/main/java/.../service/` |
| Modify neuron behavior | `worker/src/main/java/.../net/neuron/` |
| Add a new signal type | `worker/src/main/java/.../net/signals/` |
| Add a storage backend | `worker/src/main/java/.../net/storages/` |
| Add a learning algorithm | `worker/src/main/java/.../net/study/` |
| Update gRPC service | `worker/src/main/proto/jneoapalliumservice.proto` |
| Change runtime config | `worker/src/main/resources/config.properties` |
| Change dependencies | `worker/pom.xml` or `master/pom.xml` |

---

## Architecture Notes

- The **master** module is optional — only needed for HTTP/gRPC cluster modes
- The **worker** module is self-contained and can run standalone (`LocalApplication`)
- Dynamic class loading (`JarClassLoaderService`) allows injecting custom implementations at runtime without recompiling the framework
- Signal processing operates on two frequency loops: **fast** and **slow** (ratio configured in `config.properties`)
- Layer sizes can change at runtime via `ILayerManipulatingNeuron`
- All core types are interfaces — prefer programming to interfaces, not concrete implementations
