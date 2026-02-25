# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a CodeCrafters "Build Your Own Redis" challenge implementation in Java. The goal is to build a toy Redis clone that handles basic commands like PING, SET, and GET.

## Build and Run Commands

```sh
# Build and run the Redis server locally
./your_program.sh

# Build only (creates jar at /tmp/codecrafters-build-redis-java/codecrafters-redis.jar)
mvn -q -B package -Ddir=/tmp/codecrafters-build-redis-java

# Run the compiled jar directly
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

## Architecture

- **Entry point**: `src/main/java/Main.java` - Contains the main server implementation
- **Build system**: Maven with Java 25 and preview features enabled
- **Default port**: 6379 (standard Redis port)

## Key Technical Details

- Uses `--enable-preview` flag for Java preview features
- `SO_REUSEADDR` is set on the server socket to handle rapid restarts during testing
- The CodeCrafters tester restarts the program frequently, so socket reuse is important

## Project constraints
- Do not read the content of the docs folder, unless I explicit ask you to do it.
- Ignore the AGENTS.md, github.key and GEMINI.md files.
- Ignore the target folder.



