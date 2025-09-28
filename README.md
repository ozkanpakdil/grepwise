# GrepWise

An open-source alternative to Splunk for log analysis and monitoring.

## Prerequisites

Before you can run this project, you need to have the following installed:

### 1. Java Development Kit (JDK) 25

The project requires Java 25. Here's how to install it:

#### For Windows:

1. Download JDK 25 from one of these sources:
   - [Oracle JDK 25](https://www.oracle.com/java/technologies/downloads/#jdk25)
   - [Eclipse Temurin (OpenJDK) 25](https://adoptium.net/temurin/releases/?version=25)
   - [Amazon Corretto 25](https://docs.aws.amazon.com/corretto/latest/corretto-25-ug/downloads-list.html)

2. Run the installer and follow the installation instructions.

3. Set up environment variables:
   - Set `JAVA_HOME` to the JDK installation directory (e.g., `C:\Program Files\Java\jdk-25`)
   - Add `%JAVA_HOME%\bin` to your `PATH` environment variable

4. Verify the installation by opening a new command prompt and running:
   ```
   java -version
   ```
   You should see output indicating Java 25.

#### For macOS:

1. Install using Homebrew:
   ```
   brew install openjdk@25
   ```

2. Follow the instructions from Homebrew to set up your environment variables.

#### For Linux:

1. Install using your package manager, for example on Ubuntu:
   ```
   sudo apt-get update
   sudo apt-get install openjdk-25-jdk
   ```

### 2. Node.js and npm

Required for the frontend:

1. Download and install Node.js (which includes npm) from [nodejs.org](https://nodejs.org/) (LTS version recommended)
2. Verify the installation:
   ```
   node -v
   npm -v
   ```

## Running the Project

### Backend (Spring Boot)

#### Using Maven (Recommended)

1. Navigate to the project root directory:
   ```
   cd path/to/GrepWise
   ```

2. Use:
   ```
   mvn spring-boot:run
   ```

3. The backend server will start on http://localhost:8080

#### Using Gradle (Alternative)

1. Navigate to the project root directory:
   ```
   cd path/to/GrepWise
   ```

2. Build and run the application using Gradle:
   ```
   ./gradlew bootRun
   ```
   On Windows, use `gradlew.bat bootRun` instead.

3. The backend server will start on http://localhost:8080

### Frontend (React)

### Single-binary release (Spring Boot serves UI + API)

- From now on, the frontend is built and packaged into the Spring Boot JAR during `mvn package`.
- To build everything and produce a single runnable JAR that serves the UI on 8080:
  1. From project root: `mvn -DskipTests package`
  2. Run the app: `java -jar target/grepwise-0.0.1-SNAPSHOT.jar`
  3. Open http://localhost:8080

Notes:
- The build runs Node.js in `frontend/` via the Maven frontend plugin (`npm ci` + `npm run build`).
- Built assets are bundled into the JAR under `classpath:/static` and Spring Boot serves them.
- Client-side routes are handled by a fallback to `/index.html` (SPA routing) while `/api/**` and backend endpoints continue to work normally.

For local development with hot reload you can still run both:
- Backend on 8080 via `mvn spring-boot:run`
- Frontend dev server on 3000 via `npm run dev` inside `frontend/`

1. Navigate to the frontend directory:
   ```
   cd path/to/GrepWise/frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm run dev
   ```

4. The frontend will be available at http://localhost:3000

### End-to-End Tests (Playwright)

1. Start backend (8080) as above.
2. In another terminal, run E2E tests (this will auto-start the frontend dev server at 3000 by default):
```
cd path/to/GrepWise/playwright
npm install
npm run install-browsers
npm test
```

To point tests to an already running frontend at a custom URL:
```
PW_NO_SERVER=1 BASE_URL=http://localhost:3000 npm test
```
## Project Structure

- `src/main/java` - Backend Java code
- `src/main/proto` - Protocol Buffer definitions for gRPC services
- `src/main/resources` - Backend configuration files
- `frontend/` - React frontend application

## Features

- Log ingestion and storage
- Advanced log search and filtering
- Alarm creation and management
- User authentication and authorization
- Dark/light theme support
- Redaction of sensitive fields (configurable via config/redaction.json); in log details it can be revealed

## Network Log Ingestion (Syslog TCP/UDP)

GrepWise includes a built-in Syslog server that can ingest logs over the network using UDP or TCP, compatible with RFC3164 and RFC5424 formats.

- Protocols: UDP and TCP
- Default port: 1514 (unprivileged). If running as root or with CAP_NET_BIND_SERVICE, you may choose 514.
- Formats: RFC3164 (BSD) and RFC5424 (IETF)

How to enable a syslog listener via REST:

1) Create a syslog source (example uses TCP 1514, RFC5424):

POST /api/sources
Content-Type: application/json

{
  "id": "syslog-tcp-1514",
  "name": "Syslog TCP 1514",
  "enabled": true,
  "sourceType": "SYSLOG",
  "syslogPort": 1514,
  "syslogProtocol": "TCP",
  "syslogFormat": "RFC5424"
}

2) Verify it’s listed:
- GET /api/sources/type/SYSLOG

Quick manual test
- UDP: echo "<134>Oct 11 22:14:15 myhost myapp: hello via UDP" | nc -u -w1 localhost 1514
- TCP: printf "<134>1 2024-10-10T10:10:10Z myhost myapp 1234 - - hello via TCP\n" | nc localhost 1514

Forwarding from rsyslog (example):

/etc/rsyslog.d/50-grepwise.conf
*.* @@127.0.0.1:1514   # @@ for TCP, @ for UDP

Then restart rsyslog: sudo systemctl restart rsyslog

Forwarding from syslog-ng (example):

destination d_grepwise { tcp("127.0.0.1" port(1514)); };
log { source(s_src); destination(d_grepwise); };

Java app logging notes
- For log4j/logback, you can use a Syslog appender (commonly UDP). If you need TCP, route your app logs to the local OS syslog (rsyslog/syslog-ng) and forward to GrepWise over TCP as shown above.
- GrepWise parses incoming messages into structured fields (host, app, severity) and stores raw message content.

## Performance Testing (JMeter)

We include JMeter-based performance tests to benchmark GrepWise after each release.

What is covered:
- HTTP search endpoints:
  - GET /api/logs/search (with query and size)
  - GET /api/logs/count
- Network ingestion (UDP syslog on port 1514) while searching
- Combined parallel scenario: UDP ingestion + search concurrent

Planned/optional additions:
- TCP syslog test when TCP listeners are enabled
- Directory ingestion test (create a log directory via API and write files to measure ingestion-to-availability)

How to run
1) Start GrepWise (single JAR or via mvn spring-boot:run) and ensure it listens on:
   - HTTP: http://localhost:8080
   - Syslog UDP: 1514
2) From project root run:
   mvn -Pperf-test verify

Configuration
- Default properties are in src/test/jmeter/jmeter.properties. Override via -D, e.g.:
  mvn -Pperf-test -Dgw.host=localhost -Dgw.http.port=8080 -Dgw.syslog.port=1514 -Dusers=20 -DdurationSeconds=120 verify

Reports
- HTML reports per plan are generated under target/jmeter/reports/&lt;testplan&gt;/index.html
- JTL/CSV raw results are under target/jmeter/results
- During non-GUI runs, the console now prints a live summary every ~5s (p95/avg/throughput) so you can see progress. Tests run for the configured duration (default 60s) and then exit automatically.
- Compare runs across releases by storing the reports as build artifacts.

Run locally with helper scripts
- Quick start (build, start app, run tests, stop app):
  1) Make executable once: chmod +x scripts/perf/run-perf-local.sh
  2) Run: scripts/perf/run-perf-local.sh
  3) Outputs: target/jmeter/reports/… (HTML), target/jmeter/results/… (CSV), target/jmeter/perf-summary.md
- Against an already running instance:
  1) Make executable once: chmod +x scripts/perf/run-perf-against.sh
  2) Run: scripts/perf/run-perf-against.sh
- Environment overrides (examples):
  GW_HOST=localhost GW_HTTP_PORT=8080 GW_SYSLOG_PORT=1514 USERS=20 DURATION=120 RAMP_UP=30 \
  scripts/perf/run-perf-local.sh
- The local scripts also invoke scripts/perf/summarize_and_compare.py when present to produce a human-readable summary and trend comparison.

CI automation
- A GitHub Actions workflow (.github/workflows/perf-bench.yml) builds the app, runs mvn -Pperf-test verify, and uploads the HTML dashboards and CSV results as build artifacts on every push to main, on releases, and on manual dispatch.
- You can download artifacts named perf-results-<run_number> from the workflow run page. They include target/jmeter/reports and target/jmeter/results plus app.log.
- Default CI load is modest (USERS=10, DURATION=60). Adjust via env in the workflow or override Maven properties.

Scenarios list
- Single-user smoke (users=1, duration=15): quick sanity
- Read-heavy search (users=50, ramp=30s, duration=5m): latency and throughput for search endpoints
- Ingest-heavy UDP (ingest users=50, search users=5): measures ingestion rate and indexing lag while lightly searching
- Parallel balanced (ingest users=20, search users=20): overall system behavior under mixed load

Notes
- Ensure rate limiting settings allow the intended load. See RateLimitingFilter.
- Some endpoints require auth; search endpoints are permitted by default per WebSecurityConfig.

## Development

### Backend Development

- The backend is built with Spring Boot and uses gRPC for communication with the frontend
- Log data is stored in memory for now (can be extended to use a database)
- Authentication is handled using JWT tokens

### Frontend Development

- The frontend is built with React, TypeScript, and Vite
- UI components use Tailwind CSS and shadcn/ui
- State management is handled with Zustand
- The application is responsive and works on mobile devices

## Configuration

Redaction configuration
- Backend loads redaction settings from: ~/.GrepWise/config/redaction.json
- Grouped format (required):
  {
    "[\"password\",\"passwd\"]": { "patterns": [
      "(?i)(authorization\\s*:\\s*Bearer\\s+)([^\\n\\r]+)"
    ]},
    "cardnumber": { "patterns": [
      "(?i)(card(number)?\\s*[:=]\\s*)(\\b(?:\\d[ -]*?){13,19}\\b)"
    ]}
  }
  Note: JSON object keys must be strings. For multi-key groups, the property name is a JSON-stringified array (as shown above).
- If the file is missing, it will be created on startup with defaults ["password","passwd"]. If a legacy flat file is detected on startup, it will be migrated and rewritten to the grouped format automatically.
- Managing at runtime:
  - GET /api/redaction/config – returns groups (authoritative) and also convenience flattened keys/patterns
  - POST /api/redaction/config – grouped-map format only; flat {keys,patterns} is rejected
  - Legacy: GET /api/redaction/keys, POST /api/redaction/keys, POST /api/redaction/reload
- Redaction applies to search results and exports (mask *****), and alerts (mask ***). Use the Reveal button in log details to fetch unredacted content for a single row.

See docs/REDACTION.md for more examples (telephone, credit card, idnumber).

## Troubleshooting

### Java Version Issues

If you encounter errors related to Java version:

1. Make sure you have JDK 25 installed
2. Verify that `JAVA_HOME` points to JDK 25
3. Ensure that the JDK 25 bin directory is in your PATH
4. Run `java -version` to confirm you're using Java 25

### Build System Issues

#### Maven Issues

If Maven fails to run:

1. Try running with the `-X` flag for debug information:
   ```
   mvn -X spring-boot:run
   ```

2. Ensure you have Maven installed correctly:
   ```
   mvn --version
   ```

3. Check that your JAVA_HOME environment variable is set correctly to JDK 21 or higher

#### Gradle Issues

If Gradle fails to run:

1. Try running with the `--info` or `--debug` flag for more information:
   ```
   ./gradlew bootRun --info
   ```

2. Make sure you're using the Gradle wrapper (`gradlew` or `gradlew.bat`) rather than a globally installed Gradle

### Frontend Issues

If you encounter issues with the frontend:

1. Make sure you've installed all dependencies with `npm install`
2. Check for any error messages in the console
3. Try clearing your browser cache or using incognito mode


## Performance trend, visuals, and alerts in CI

- Every run of the Performance Benchmarks workflow produces:
  - A human-readable summary with tables and colored indicators in the workflow run summary (p95 latency per scenario vs moving average).
  - HTML dashboards under artifacts: target/jmeter/reports/…
  - Raw CSV results under artifacts: target/jmeter/results/…
  - A JSON and Markdown summary under target/jmeter/ (perf-summary.json / perf-summary.md).
  - A cumulative history file at docs/perf/history.csv and a status badge at docs/perf/badge.svg (committed to main). Changes to docs/perf/** do not retrigger the perf workflow.

- Alert policy (based on p95 latency vs the last 10-run moving average per scenario):
  - <= 5% change: OK (green)
  - > 5% and <= 10%: Yellow warning (investigate)
  - > 10% and <= 20%: Red alert (highlighted, but build continues)
  - > 20%: Blocking red alert (the job fails to prevent release)

Notes
- The history file accumulates results across runs on main. You can reset it if needed by editing docs/perf/history.csv.
- The badge reflects the worst status across scenarios in the latest run (OK/WARN/ALERT).
