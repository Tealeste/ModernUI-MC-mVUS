# Compatibility Layer Proposal (ModernUI-MC)

This document proposes a small, explicit compatibility layer (“compat layer”) to reduce long-term duplication across loader targets (Fabric / Forge / NeoForge) and across API/version drift, while keeping changes minimal-risk and easy to roll back.

## Goals
- Minimize loader/version duplication in shared code paths.
- Keep behavior stable across supported targets.
- Prefer small, incremental migrations over large rewrites.
- Keep compatibility logic discoverable (one place to look).

## Non-goals
- No new external runtime dependencies.
- No repo-wide refactors or large API redesigns.
- No changes to mixin targets or rendering pipelines as part of early migrations.

## Approach (Compat Layer “Shape”)

### A) Common-layer shims (`common/`)
Use when:
- The APIs are conceptually the same across loaders (often only package-name differences).
- A safe fallback exists when the API is absent.

Implementation pattern:
- Reflection/MethodHandles resolved once (static init).
- Expose a small static method with a stable signature.
- Fail-safe default behavior (avoid spammy logging).

Location convention:
- `common/src/main/java/icyllis/modernui/mc/compat/*Compat.java`

### B) Loader-provided adapters (`forge/`, `neoforge/`, `fabric/`)
Use when:
- Behavior differs semantically across loaders (not just signatures/packages).
- Lifecycle integration is required (event buses, mod container wiring, etc.).

Implementation pattern:
- Define a small interface (or minimal entrypoints) in `common/`.
- Provide one implementation per loader module.
- Keep the boundary narrow so rollbacks are simple.

## Migration Plan (Phased)

### Phase 0: Document + pilot (this change)
- Add minimal compat doc.
- Add one shim + one pilot usage to demonstrate the pattern.

### Phase 1: Low-risk API drift
Candidates:
- Mod presence detection (Forge/NeoForge `ModList`, Fabric `FabricLoader`).
- Setup warnings (Forge `ModLoadingWarning` vs NeoForge `ModLoadingIssue`) via a tiny shared reporter API.
- Small, well-contained signature drift already handled ad-hoc.

### Phase 2: Consolidate duplicated loader entrypoints
Candidates:
- Loader “registration/setup” flows that differ primarily in event types and bus wiring.

### Phase 3: Optional deeper refactors (only with runtime validation)
Candidates:
- Larger duplication hotspots where behavior is currently the same but implemented 3x.

## Acceptance Criteria (For Each Migration)
- Behavior unchanged (refactor-only) unless explicitly planned.
- The change is easily revertible (localized diffs, minimal blast radius).
- Compiles for affected modules (`:common` + the touched loader modules).
- No new runtime dependencies.

## Rollback Strategy
- Replace `*Compat` call sites with the previous direct API calls.
- Delete the compat class if it becomes unused.
- Keep each migration in its own atomic commit so it can be reverted cleanly.

## Pilot (This Repository)

### `ModListCompat`
- File: `common/src/main/java/icyllis/modernui/mc/compat/ModListCompat.java`
- Purpose: provide a stable `isLoaded(String)` wrapper that works on Forge and NeoForge.
- Technique: resolve `net.minecraftforge.fml.ModList` vs `net.neoforged.fml.ModList` via MethodHandles and expose `isLoaded(String)`.

Pilot usage (behavior-preserving):
- `forge/src/main/java/icyllis/modernui/mc/forge/ModernUIForge.java` uses `ModListCompat.isLoaded(...)` for mod-presence checks.
- `neoforge/src/main/java/icyllis/modernui/mc/neoforge/ModernUIForge.java` uses `ModListCompat.isLoaded(...)` for mod-presence checks.

