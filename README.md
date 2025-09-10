# GrepWise

An open-source alternative to Splunk for log analysis and monitoring.

## Prerequisites

Before you can run this project, you need to have the following installed:

### 1. Java Development Kit (JDK) 21

The project requires Java 21. Here's how to install it:

#### For Windows:

1. Download JDK 21 from one of these sources:
   - [Oracle JDK 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
   - [Eclipse Temurin (OpenJDK) 21](https://adoptium.net/temurin/releases/?version=21)
   - [Amazon Corretto 21](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)

2. Run the installer and follow the installation instructions.

3. Set up environment variables:
   - Set `JAVA_HOME` to the JDK installation directory (e.g., `C:\Program Files\Java\jdk-21`)
   - Add `%JAVA_HOME%\bin` to your `PATH` environment variable

4. Verify the installation by opening a new command prompt and running:
   ```
   java -version
   ```
   You should see output indicating Java 21.

#### For macOS:

1. Install using Homebrew:
   ```
   brew install openjdk@21
   ```

2. Follow the instructions from Homebrew to set up your environment variables.

#### For Linux:

1. Install using your package manager, for example on Ubuntu:
   ```
   sudo apt-get update
   sudo apt-get install openjdk-21-jdk
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

1. Make sure you have JDK 21 installed
2. Verify that `JAVA_HOME` points to JDK 21
3. Ensure that the JDK 21 bin directory is in your PATH
4. Run `java -version` to confirm you're using Java 21

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
