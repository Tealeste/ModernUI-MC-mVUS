# Plan — Add Minecraft `1.21.9` Build Compatibility (Baseline: `1.21.10`)

## Objective
Add full backward compatibility for Minecraft `1.21.9` to the existing multi-version Gradle build, using the current `1.21.10` support as the template/baseline, while keeping all currently supported versions working.

## Tools Available (session)
- `functions.exec_command({ cmd, workdir?, yield_time_ms?, max_output_tokens?, login? })` — run shell commands
- `multi_tool_use.parallel({ tool_uses: [...] })` — run multiple tool calls in parallel
- `functions.apply_patch(<patch>)` — edit files via patch (freeform)
- `functions.update_plan({ plan: [{ step, status }], explanation? })` — update the interactive plan UI
- `functions.mcp__ace-tool__search_context({ project_root_path, query })` — semantic repo search (when `rg`/file pointers are insufficient)
- `functions.mcp__sequential-thinking__sequentialthinking(...)` — internal step reasoning (no chain-of-thought will be output)
- `functions.mcp__exa__web_search_exa(...)` / `functions.mcp__exa__get_code_context_exa(...)` — web lookups (only if repo evidence is insufficient; queries must be sanitized)

## Acceptance Contract (thin)
### Contracts / Interfaces Touched
- Gradle property: `minecraft_version` (selected via `-Pminecraft_version=<version>`, default from `gradle.properties`)
- Version profile map: `ext.modernUiMcProfiles` in `build.gradle` (add key `1.21.9`)
- Loader metadata expansion (already pattern-based):
  - Fabric: `fabric/src/main/resources/fabric.mod.json` via `fabric/build.gradle` `processResources`
  - Forge: `forge/src/main/resources/META-INF/mods.toml` via `forge/build.gradle` `processResources`
  - NeoForge: `neoforge/src/main/resources/META-INF/neoforge.mods.toml` via `neoforge/build.gradle` `processResources`
- Release/distribution task: `buildReleaseJars` (defined in `build.gradle`)

### Expected Behavior
- Gradle accepts `-Pminecraft_version=1.21.9` using the same profile mechanism as `1.21.10`.
- `./gradlew buildReleaseJars -Pminecraft_version=1.21.9` succeeds and produces release jars in `build/release/`.
- `./gradlew buildReleaseJars -Pminecraft_version=1.21.10` still succeeds (no regressions).
- Fabric/Forge/NeoForge metadata version ranges for `1.21.9` are correctly expanded according to existing patterns.

### Explicit Non‑Goals
- No new dependencies, no major version bumps, no refactors unrelated to version selection/build compatibility.
- No changes to publishing credentials or signing behavior.

### Edge Cases / Backward Compatibility Constraints
- Preserve existing supported targets and their dependency pins.
- Ensure patch-level source set routing remains correct (`mcPatch` logic in each module).

## Step Plan (execute in order; update status + evidence after each)

### 1) Discovery (profiles + tasks + baseline)
- **Status:** DONE
- **Actions:**
  - Confirm where supported MC versions are defined and how `minecraft_version` is selected.
  - Confirm how `1.21.10` is configured (dependencies + metadata ranges).
  - Recover repo-historical `1.21.9` profile values (if present) for evidence-based pins.
  - Confirm Java/Gradle environment (`./gradlew --version`).
- **Verification:** capture command outputs below.

### 2) Root Gradle profile: add `1.21.9`
- **Status:** DONE
- **Actions:**
  - Add `1.21.9` entry to `ext.modernUiMcProfiles` in `build.gradle`, mirroring the `1.21.10` structure.
  - Pin dependency versions using repo evidence (prefer repo history when current tree lacks `1.21.9`).
- **Verification:** `./gradlew -q projects` (sanity), plus later full build in Step 8.

### 3) `:common` (shared code) — ensure `1.21.9` routing works
- **Status:** DONE
- **Actions:**
  - Confirm `mcPatch` routing covers patch `9` (pre-`1.21.10` sources) and compiles for `1.21.9`.
  - Make changes only if compilation reveals a real API drift issue.
- **Verification:** part of Step 8 build; optionally `./gradlew :common:compileJava -Pminecraft_version=1.21.9`.

### 4) `:ModernUI-Fabric` — ensure `1.21.9` build + metadata
- **Status:** DONE
- **Actions:**
  - Confirm `processResources` expands Fabric `minecraft_range` based on the selected target.
  - Ensure compile succeeds for `1.21.9` and produces `remapJar`.
- **Verification:** `./gradlew :ModernUI-Fabric:remapJar -Pminecraft_version=1.21.9`.

### 5) `:ModernUI-Forge` — ensure `1.21.9` build + metadata
- **Status:** DONE
- **Actions:**
  - Confirm Forge version pin and `mods.toml` `minecraft_version_range` expansion for `1.21.9`.
  - Ensure `remapJar` succeeds for `1.21.9`.
- **Verification:** `./gradlew :ModernUI-Forge:remapJar -Pminecraft_version=1.21.9`.

### 6) `:ModernUI-NeoForge` — ensure `1.21.9` build + metadata
- **Status:** DONE
- **Actions:**
  - Confirm NeoForge version pin and `neoforge.mods.toml` `minecraft_version_range` expansion for `1.21.9`.
  - Ensure `remapJar` succeeds for `1.21.9`.
- **Verification:** `./gradlew :ModernUI-NeoForge:remapJar -Pminecraft_version=1.21.9`.

### 7) Documentation (repo-local)
- **Status:** DONE
- **Actions:**
  - Update `README.md` / `README.en.md` supported MC version list to include `1.21.9`, matching the build profiles.
- **Verification:** none (doc-only).

### 8) Full verification (no regressions)
- **Status:** DONE
- **Commands:**
  - `./gradlew buildReleaseJars -Pminecraft_version=1.21.9`
  - `./gradlew buildReleaseJars -Pminecraft_version=1.21.10`
  - (Optional if feasible) dev-client smoke test for `1.21.9`:
    - `./gradlew :ModernUI-Fabric:runClient -Pminecraft_version=1.21.9`
    - `./gradlew :ModernUI-Forge:runClient -Pminecraft_version=1.21.9` (or `:ModernUI-NeoForge:runClient`)

## Evidence Log (append per step)
### Step 1
- Located version selection + profiles: `build.gradle` defines `ext.modernUiMcProfiles` and selects via `minecraft_version` (`findProperty('minecraft_version') ?: '1.21.11'`).
- Confirmed default target: `gradle.properties` sets `minecraft_version=1.21.11`.
- Recovered repo-historical `1.21.9` pins from `git show a96751cd^:build.gradle`:
  - `forge: 59.0.5`
  - `neoforge: 21.9.16-beta`
  - `fabricLoader: 0.18.1`
  - `fabricApi: 0.134.1+1.21.9`
  - `modMenu: 16.0.0-rc.2`
  - `forgeConfigApiPortFabric: 21.9.7`
- Environment check:
  - `./gradlew --version` failed because wrapper requires Java 21; detected Java `25.0.1` with `JAVA_HOME` unset.
  - JDK 21 is installed at `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` (from `/usr/libexec/java_home -V`); all Gradle commands will be run with `JAVA_HOME` set to this path.
### Step 2
- Implemented `1.21.9` profile in `build.gradle` by copying the `1.21.10` structure and applying repo-historical pins (from Step 1 evidence).
- Sanity check (Gradle evaluation succeeds with Java 21):
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew -q projects -Pminecraft_version=1.21.9` ✅
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew -q projects -Pminecraft_version=1.21.10` ✅
### Step 3
- Initial `:common:compileJava -Pminecraft_version=1.21.9` failed when `mcPatch < 10` routed `1.21.9` to `src/pre_1_21_10/java` (API mismatches vs 1.21.9).
- Fix: adjusted `common/build.gradle` source-set routing to use `src/pre_1_21_10/java` only for `mcPatch < 9` (so `1.21.8` stays on the old sources; `1.21.9+` uses the same sources as `1.21.10`).
- Verification:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :common:compileJava -Pminecraft_version=1.21.9` ✅
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :common:compileJava -Pminecraft_version=1.21.8` ✅ (unchanged support)
### Step 4
- Updated `fabric/build.gradle` source-set routing to match `:common` (`mcPatch < 9` uses `src/pre_1_21_10/java`; `1.21.9+` uses the same sources as `1.21.10`).
- Verification:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :ModernUI-Fabric:remapJar -Pminecraft_version=1.21.9` ✅
  - Expanded metadata check: `fabric/build/resources/main/fabric.mod.json` contains `"minecraft": ">=1.21.9 <1.21.10"` ✅
### Step 5
- Updated `forge/build.gradle` source-set routing to match `:common`/Fabric (`mcPatch < 9` uses `src/pre_1_21_10/java`; `1.21.9+` uses the same sources as `1.21.10`).
- Verification:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :ModernUI-Forge:remapJar -Pminecraft_version=1.21.9` ✅
  - Expanded metadata check: `forge/build/resources/main/META-INF/mods.toml` contains `versionRange="[1.21.9, 1.21.10)"` for `modId="minecraft"` ✅
### Step 6
- Updated `neoforge/build.gradle` source-set routing to match `:common`/Fabric/Forge (`mcPatch < 9` uses `src/pre_1_21_10/java`; `1.21.9+` uses the same sources as `1.21.10`).
- Verification:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :ModernUI-NeoForge:remapJar -Pminecraft_version=1.21.9` ✅
  - Expanded metadata check: `neoforge/build/resources/main/META-INF/neoforge.mods.toml` contains `versionRange="[1.21.9, 1.21.10)"` for `modId="minecraft"` ✅
### Step 7
- Updated supported MC version list to include `1.21.9`:
  - `README.en.md`
  - `README.md`
### Step 8
- Full build verification (Java 21):
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew buildReleaseJars -Pminecraft_version=1.21.9` ✅ (`BUILD SUCCESSFUL`)
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew buildReleaseJars -Pminecraft_version=1.21.10` ✅ (`BUILD SUCCESSFUL`)
- Output location (per repo build logic): `build/release/` (note: `syncReleaseJars` is a Gradle `Sync` task and replaces contents per target run).
- Dev-client smoke test for `1.21.9` was not run here because it is interactive/GUI-bound and would block the automated session; run manually if desired:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :ModernUI-Fabric:runClient -Pminecraft_version=1.21.9`
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew :ModernUI-Forge:runClient -Pminecraft_version=1.21.9` (or `:ModernUI-NeoForge:runClient`)
