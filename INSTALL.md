# GrepWise Installation and Local Run Guide

This guide explains how to install and run GrepWise using either:
- Prebuilt native executables (recommended) from [GitHub Releases](https://github.com/ozkanpakdil/grepwise/releases) for Linux, and Windows
- The portable Spring Boot JAR (requires a JDK)

You can also build from source if you prefer.


## 1) Download from GitHub Releases (recommended)

When a new version is tagged (vX.Y.Z), the CI publishes a GitHub Release with:
- Native executables:
  - Linux: `grepwise` (executable)
  - Windows: `grepwise.exe`
- Spring Boot JAR: `grepwise-<version>.jar`

Steps:
1. Go to the [Releases page](https://github.com/ozkanpakdil/grepwise/releases) of the repository.
2. Download the asset for your operating system (or the JAR if you prefer).


### Linux
1. Download `grepwise` from the release assets.
2. Make it executable and run:
   - `chmod +x ./grepwise`
   - `./grepwise`

If you see a security prompt, open System Settings → Privacy & Security and allow the binary to run.

### Windows
1. Download `grepwise.exe` from the release assets.
2. Double‑click to run, or use PowerShell/CMD:
   - `.
   grepwise.exe`


## 2) Run with JAR (requires JDK)

If you prefer the portable JAR:
1. Ensure you have a recent JDK (24+ recommended; 25 is used in CI).
   - Verify with: `java -version`
2. Download `grepwise-<version>.jar` from Releases.
3. Run:
   - `java -jar grepwise-<version>.jar`


## 3) Configuration

Default configuration lives under:
- `src/main/resources/application.properties` (defaults baked into the app)
- You can override via environment variables or an external `application.properties` placed in the working directory.

Common overrides (examples):
- `SERVER_PORT=8080`
- `SPRING_PROFILES_ACTIVE=prod`

Pass as environment variables or via `--server.port=8080` style command‑line arguments.

Examples:
- Native binary: `SERVER_PORT=9090 ./grepwise --spring.profiles.active=prod`
- JAR: `SERVER_PORT=9090 java -jar grepwise-<version>.jar --spring.profiles.active=prod`


## 4) Build from Source (optional)

Requirements:
- Git
- Maven wrapper (included) and internet access
- For native builds: GraalVM with native‑image (CI builds for you, but local builds require setup)

Steps:
1. Clone the repo:
   - `git clone https://github.com/<owner>/grepwise.git`
   - `cd grepwise`
2. Build the Spring Boot JAR:
   - `mvn -DskipTests package`
   - The JAR will be in `target/grepwise-<version>.jar`
3. Build a native image (Linux):
   - Install GraalVM (Community or Enterprise) and native‑image
   - `mvn -Pnative -DskipTests package`
   - Output binary will be in `target/grepwise` (Windows: `target/grepwise.exe`)

Windows local native build notes:
- Ensure Microsoft Visual Studio Build Tools (C/C++) are installed and available in PATH.


## 5) Running Locally

- Start the app:
  - Native: `./target/grepwise` (or `./grepwise` if you’re in the release asset directory)
  - JAR: `java -jar target/grepwise-<version>.jar`
- Then open your browser at: `http://localhost:8080`

Logs will be printed to the console by default; check the repository README for additional runtime notes and plugin/config details.


## 6) Troubleshooting

- Port already in use: Change with `--server.port=<port>` or set `SERVER_PORT` env var.
- Windows missing MSVC when building native: Install Visual Studio Build Tools with C++ workload.
- GraalVM native build errors: Ensure you’re using a GraalVM version compatible with your JDK (CI uses latest GraalVM with Java 25) and that `native-image` is installed.

If you hit issues, please open a GitHub issue with logs and your OS/JDK details.