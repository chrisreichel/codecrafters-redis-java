# CodeCrafters Redis Java Project

## Project Overview
This is a Java implementation of a Redis clone, built as part of the CodeCrafters challenge. The project uses Java 25 with preview features and is managed with Maven.

### Main Technologies
- **Java 25**: Utilizes modern Java features, including preview features enabled via `--enable-preview`.
- **Maven**: Handles dependency management and building.
- **Redis Protocol**: Implements the Redis Serialization Protocol (RESP) to handle client connections and commands.

## Building and Running
- The project includes scripts to simplify building and running both locally and in the CodeCrafters environment.
- Everytime you run mvn, make sure to run with -q, just supress the flag for troubleshooting

### Key Commands
- **Build and Run Locally**:
  ```bash
  ./your_program.sh
  ```
  This script compiles the project using Maven and then executes the JAR.

- **Manual Build (Maven)**:
  ```bash
  mvn package -Ddir=/tmp/codecrafters-build-redis-java
  ```
  This command packages the application into a JAR with dependencies at `/tmp/codecrafters-build-redis-java/codecrafters-redis.jar`.

- **Manual Run**:
  ```bash
  java --enable-preview -jar /tmp/codecrafters-build-redis-java/codecrafters-redis.jar
  ```

## Test
```sh
codecrafters test
```

## Submitting to CodeCrafters

```sh
codecrafters submit
```

## Coding Standards
- Follow SOLID principles — one responsibility per class
- Use constructor injection (never field injection with @Autowired)
- Services must have interfaces; implementations go in a `impl/` subfolder
- All public methods require Javadoc
- Use Optional<> instead of returning null
- Favor immutability, set variables as final whenever you can

## Agent Team Workflow
This project uses specialized sub-agents. Follow this pipeline for new features:
1. **architect** – Review design before coding starts
2. **implementer** – Write the code
3. **tester** – Write JUnit 5 tests
4. **auditor** – Security review before merge

Every time you implement something, run the mvn clean test to make sure the code compiles and the tests are running, then do a git commit. Add a meaningful commit message describing the changes.


### Configuration
- `codecrafters.yml`: Configures the CodeCrafters build environment (e.g., `buildpack: java-25`).
- `pom.xml`: Contains the Maven configuration, including the target Java version and the `maven-assembly-plugin` for creating a FAT JAR.

## Development Conventions
- **Main Class**: The entry point is `Main.java` located in `src/main/java/`.
- **Java Version**: Always use Java 25 with `--enable-preview`.
- **Artifact Name**: The artifact name `codecrafters-redis` in `pom.xml` should not be changed as the build scripts depend on it.
- **Port**: The server defaults to port `6379`.
- **Socket Reuse**: The `ServerSocket` should have `setReuseAddress(true)` to avoid "Address already in use" errors during frequent tester restarts.

## Project constraints
- Do not read the content of the docs folder, unless I explicit ask you to do it.
- Ignore the AGENTS.md, github.key and CLAUDE.md files.
- Ignore the target folder.

