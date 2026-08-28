# ooniprobe CLI (`:cliApp`)

A JVM command-line front-end for OONI Probe, aiming for parity with the Go
[`ooni/probe-cli`](https://github.com/ooni/probe-cli) `ooniprobe` command.

## Architecture

`:cliApp` is a thin Kotlin/JVM application built with [Clikt](https://ajalt.github.io/clikt/). It
depends **only on `:probeCore`** — never on `:composeApp` — so it carries no Compose UI, resources,
navigation, or view models. Measurement, upload, storage, onboarding, geolocation, and autorun all
run through the shared engine and orchestration in `:probeCore`, the same code the mobile and desktop
apps use.

For packaged behavior, verify with the installed start script (`installDist`) rather than
`runDebug`/`run`, so the real distribution classpath (including the bundled descriptor assets) is
exercised.

## Commands

| Command | Notes |
|---|---|
| `version` | Print the CLI version. |
| `info` | Print resolved paths and runtime settings (`--json`). |
| `list [resultId]` | List results, or the measurements of a result (`--json`). |
| `show <measurementId>` | Print a stored measurement record as JSON. |
| `rm <resultId>` / `rm --all` | Delete results (`--yes` or interactive confirmation). |
| `reset --force` | Delete all local OONI Probe data (database, files, and stored credentials). |
| `onboard [--yes]` | Run the informed-consent onboarding, or auto-accept. |
| `upload [all\|result <id>\|measurement <id>]` | Upload measurements not yet submitted. |
| `geoip` | Print the probe's IP, ASN, network name, and country (`--json`). |
| `run [group]` | Run measurement groups (see below). |
| `autorun status \| log show \| log stream` | Inspect autorun readiness and logs. |
| `help` | Print usage. |

### `run` groups

Groups: `websites`, `im`, `performance`, `circumvention`, `middlebox`, `experimental`, `unattended`,
`all`. Bare `run` is equivalent to `run all`.

- `run websites` accepts repeatable `--input <url>` and `--input-file <path>`. Inputs are used in
  order (all `--input` values first, then each file's non-blank lines); any invalid URL aborts the
  run with a file/line diagnostic before measuring.
- `--no-collector` skips uploading results; `--no-creds` skips credential preparation and uses the
  anonymous submission path.
- Running requires completed onboarding; in `--batch` mode an incomplete onboarding fails with a
  message to run `ooniprobe onboard --yes`.

## Root flags

`--config/-c <file>`, `--verbose/-v`, `--batch`, `--log-handler={cli,batch,syslog}`,
`--software-name`, `--software-version`, `--proxy`, `--json`. Global flags may appear before or after
the subcommand.

## Software name

The identity reported to the OONI backend (measurements, check-in, geolocation) is `ooniprobe-cli`,
distinct from the desktop app's `ooniprobe-desktop`. Auto-run measurements report
`ooniprobe-cli-unattended`.

## Paths

OONI home resolution: the `OONI_HOME` environment variable, else `${user.home}/.ooniprobe`.
`--config <file>` selects the config file only; it does not redirect the home. Data lives under
`<ooniHome>/data`, logs under `<ooniHome>/logs`.

Onboarding and autorun preferences persist to `<ooniHome>/data/probe.preferences.json` via
`JsonFilePreferencesDataStore` (`org.ooni.probe.core`), not Android DataStore's protobuf format —
see [issue #1546](https://github.com/ooni/probe-multiplatform/issues/1546) for why.

## Build, run, test

```bash
# Unit tests (fast; no device or simulator)
./gradlew :cliApp:test :probeCore:desktopTest

# Install and run the real executable (recommended for smoke checks)
./gradlew :cliApp:installDist
BIN=cliApp/build/install/ooniprobe/bin/ooniprobe
"$BIN" version
"$BIN" help
OONI_HOME=$(mktemp -d) "$BIN" list --json
OONI_HOME=$(mktemp -d) "$BIN" onboard --yes
```

Tests never touch the real `~/.ooniprobe`; they inject a temporary `OONI_HOME` and temp paths.

## Native image (GraalVM)

`:cliApp` also builds as a single native executable via the
[GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/) plugin:

```bash
./gradlew :cliApp:nativeCompile
BIN=cliApp/build/native/nativeCompile/ooniprobe
"$BIN" version
```

Requires a GraalVM JDK matching the module's `jvmTarget` (currently 25) — mismatched JDK versions
between the toolchain and `native-image` fail the build. `.github/workflows/cli-native-build.yml`
builds and uploads the binary for macOS, Linux, and Windows on every push/PR touching `cliApp`,
`probeCore`, or `desktopShared`.

Reachability metadata (reflection/JNI/resources needed by the closed-world native build) lives
under `src/main/resources/META-INF/native-image/org.ooni.probe.cli/`, split across two files:

- `reachability-metadata.json` — the unified modern format: reflection/JNI (`oonimkall`/gomobile Go
  bridge, the `uniffi.ooniprobe`/passport bridge, JNA, sqlite-jdbc) plus exact bundled-native-library
  resource paths, hand-curated and agent-captured together.
- `resource-config.json` — kept separate on purpose: its regex `pattern` resource includes (bundling
  `linux/*`/`macos/*`/`windows/*`/`jniLibs/*`/`org/sqlite/native/*`/`assets/descriptors/*.json`/etc.
  for all platforms/architectures) are silently ignored if moved into `reachability-metadata.json`'s
  `resources` array, which only honors exact `glob` entries, not `pattern` regexes.

To regenerate or extend the reflection/JNI side after touching a code path with new reflection/JNI
usage, run each affected subcommand through GraalVM's native-image tracing agent, then merge its
output in:

```bash
./gradlew -Pagent :cliApp:run --args="<subcommand> ..."
./gradlew :cliApp:metadataCopy --task run --dir=src/main/resources/META-INF/native-image/org.ooni.probe.cli
./gradlew :cliApp:nativeCompile   # rebuild and manually smoke-test the affected subcommand(s)
```

GraalVM's tracing agent only observes Java-level reflection — it can't see native code (the
Go/gomobile bridge, or JNA `Structure` subclasses like `uniffi.ooniprobe.*`) calling back into the
JVM. Gaps there only show up as a crash in the compiled binary, not in the agent's trace or in
`:cliApp:run`; diagnose from the exception (`JNIFunctions$Support.getMethodID` NPE,
`Structure.getFieldOrder()` mismatch, or a "Cannot reflectively access the proxy class" error, which
GraalVM prints the exact fix for) and register the missing class in `reachability-metadata.json` by
hand.

## Exit codes

- `0` — success.
- `2` — usage/validation errors (bad option, missing/invalid argument, `reset` without `--force`,
  a destructive command without `--yes` in `--batch`).
- `1` — runtime errors (result/measurement not found, secure-storage clear failure during `reset`,
  an unsupported operation, or an unexpected failure).
- `130` — interrupted by SIGINT/SIGTERM; the in-flight run or upload is cancelled cleanly before exit.

## Deviations from Go probe-cli

- `show` prints the **stored measurement record** (metadata + test keys) as JSON, because the raw
  report file is deleted after upload; Go probe-cli prints the raw measurement JSON.
- `reset --force` clears the local OONI home (database, cache, logs, tunnel, assets) and the stored
  OONI credentials, and preserves an external `--config` file located outside the home.
- `--help`/`-h` on a subcommand exits `2` (help is handled by the root command); `help` and the root
  `--help` exit `0`.

## Known limitations

- `run experimental` currently runs STUN Reachability, OpenVPN, and Vanilla Tor. DNS Check, ECH
  Check, and Tor Snowflake are not yet available in the bundled descriptors/engine.
- `autorun start` and `autorun stop` (OS service supervision) are not implemented and return a
  deterministic "unsupported on this platform" error; `autorun status` and `autorun log` work.
