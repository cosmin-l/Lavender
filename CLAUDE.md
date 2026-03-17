# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Lavender** is a Java project built with Gradle. Group: `org.cl`, version: `1.0-SNAPSHOT`.

## Commands

```bash
./gradlew build       # Compile and run all checks
./gradlew test        # Run tests
./gradlew clean       # Clean build artifacts
./gradlew test --tests "com.example.MyTest"  # Run a single test class
./gradlew test --tests "com.example.MyTest.myMethod"  # Run a single test method
```

## Stack

- **Java** with the Gradle `java` plugin
- **JUnit Jupiter 5.10.0** for testing (`useJUnitPlatform()`)
- **Gradle 9.2.0** (via wrapper)

## Structure

- `src/main/java/` — application source
- `src/test/java/` — unit tests
