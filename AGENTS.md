# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/`: Java source code. The entry point is `br.net.reichel.redis.Main` (see `pom.xml`).
- `src/test/java/`: JUnit 5 tests. Add package folders under `br/net/reichel/...` to match main code.
- `pom.xml`: Maven build config, Java 25, preview features, and packaging settings.
- `codecrafters.yml`: CodeCrafters build config.
- `your_program.sh`: Local build/run wrapper used by CodeCrafters.

## Build, Test, and Development Commands
- `./your_program.sh`: Build and run the Redis server locally.
- `mvn -q -B package -Ddir=/tmp/codecrafters-build-redis-java`: Build a fat JAR at `/tmp/codecrafters-build-redis-java/codecrafters-redis.jar`.
- `java --enable-preview -jar /tmp/codecrafters-build-redis-java/codecrafters-redis.jar`: Run the compiled JAR.
- `codecrafters test`: Run the CodeCrafters test suite.
- `codecrafters submit`: Submit the current solution.

## Coding Style & Naming Conventions
- Java 25 with `--enable-preview` is required (configured in `pom.xml`).
- Use standard Java naming: `UpperCamelCase` for classes, `lowerCamelCase` for methods/fields.
- Keep packages under `br.net.reichel.redis` to match the configured main class.
- Favor small, single-responsibility classes and constructor injection.

## Testing Guidelines
- Framework: JUnit Jupiter (JUnit 5).
- Place tests in `src/test/java` mirroring the main package structure.
- Name tests clearly for the behavior under test (e.g., `RespParserTest`).
- Run `mvn test` or `codecrafters test` before submitting.

## Commit & Pull Request Guidelines
- Commit messages are short and imperative. Example: `Package refactoring` or `Add integration test suite`.
- CodeCrafters submissions often use `codecrafters submit [skip ci]`.
- PRs (if used) should include a brief summary, the commands run (e.g., `mvn test`), and the CodeCrafters stage/behavior validated.

## Configuration & Runtime Notes
- The artifact name `codecrafters-redis` in `pom.xml` should not be changed; build scripts depend on it.
- The server default port is `6379`; keep socket reuse in mind for fast restarts during tests.
