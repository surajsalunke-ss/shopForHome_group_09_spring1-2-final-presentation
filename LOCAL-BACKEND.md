# Run the backend locally on Windows

This setup uses a project-local Java 11 JDK, Maven, and an embedded H2 database. No MySQL server, SMTP credentials, cloud credentials, or globally installed Java/Maven are required.

## Prerequisites

- Windows x64, Windows PowerShell 5.1 or later, Git, and `curl.exe` on PATH.
- A writable checkout and internet access to GitHub and Maven Central for the initial tool/dependency downloads.
- An available loopback port (8080 by default).

The scripts resolve paths from their own location. Clone into any writable directory, including one with spaces. From the repository root:

```powershell
Set-Location -LiteralPath .\backend
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup-local.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Start
```

The setup script downloads pinned Temurin JDK `11.0.32.1+1` and Apache Maven `3.9.16` archives, verifies their checksums, and extracts them under the repository's `.tools` directory. Re-running setup reuses installed tools. The execution-policy switch applies only to the launched PowerShell process. No administrator shell, global environment changes, or permanent execution-policy changes are required.

`Build` runs `clean verify` with test failures enforced. Maven uses the checked-in `backend/config/maven-settings.xml` and `.tools/repository`, without loading user Maven settings. The scripts select the local Java installation even if the machine's JAVA_HOME is stale.

## Start, check, rebuild, and stop

Run from the `backend` directory:

```powershell
# Start a background process and wait for HTTP 200 from the product endpoint.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Start

# Check process state and the API response.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Status
Invoke-RestMethod -Uri 'http://127.0.0.1:8080/product'

# Stop only the recorded backend process.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Stop

# Rebuild after making changes, then restart.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Start
```

Do not start twice; stop the existing instance before rebuilding. Closing the terminal does not stop the background process. `Stop` checks the executable path and process start time before using its recorded PID.

For another port, stop first and use:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\local.ps1 Start -Port 8081
Invoke-RestMethod -Uri 'http://127.0.0.1:8081/product'
```

Logs and PID metadata are written under `.local` at the repository root: `backend.stdout.log`, `backend.stderr.log`, and `backend-process.json`.

## Safe local configuration

The launcher binds to `127.0.0.1` and explicitly loads only `backend/config/local.properties` with the `local` profile. Inherited Spring and JVM/Maven override variables are cleared for the child process and restored afterward. Use these launch commands to get that isolation; a bare JAR invocation or ordinary `mvn spring-boot:run` does not perform the same setup.

Hibernate creates and updates the embedded H2 schema in `.local/database/shopforhome.mv.db`. The database persists across restarts. Tests use separate in-memory H2 databases and do not alter local development data. The H2 console is disabled.

**The initial catalog is empty.** A new checkout returns HTTP 200 from `/product` with an empty `content` array and `totalElements: 0`. No accounts, products, MySQL data, or local smoke-test credentials are shipped. Integration-test records are temporary fixtures.

The local mail sender rejects both simple and MIME delivery before opening an SMTP connection. The existing `/sendMail` endpoint reports `Error while Sending Mail` in this mode; that is expected. No mail is delivered.

The historical credential-bearing `backend/src/main/resources/application.properties` and previously tracked `backend/target` output are removed from version control. Existing local copies may remain ignored. Private configuration, credentials, tool downloads, caches, database files, logs, and machine-specific runtime files must remain untracked. The safe local configuration, Maven settings, wrapper scripts/properties, setup script, and tests are intentionally versioned. Removing files from tracking does not remove them from older Git history.

## Build changes and tests

- Replace the unavailable Spring Boot snapshot parent with `2.2.13.RELEASE`, retaining the Java 11 target.
- Put the matching Spring Boot Maven plugin under build plugins; remove incorrectly declared build/deployment plugin dependencies and obsolete Spring snapshot/milestone repositories.
- Enforce Surefire test failures and restore Maven Wrapper `3.3.4` with a pinned Maven distribution and SHA-256 checksum.
- Add integration coverage for H2/configuration isolation, persisted product reads, registration/JDBC authentication/JWT profile access, unauthorized requests, and blocked simple/MIME mail delivery.
- Point the historical context-load test at the current application class and a separate local in-memory database so it also runs in a clean checkout. Two historical test classes contain only empty methods and are retained; their passing status provides no business-feature coverage.

Test reports are generated under `backend/target/surefire-reports`. The executable JAR is `backend/target/shop-api-0.0.1-SNAPSHOT.jar`.

## Limitations

H2 is for local development. These tests do **not** validate MySQL schema/data compatibility, SMTP integration, or GCP/App Engine deployment. The historical framework and existing authorization behavior are retained; this is not a production-hardening change. Keep the development service on loopback. Frontend modernization, cloud deployment, and production migration are outside this setup.
