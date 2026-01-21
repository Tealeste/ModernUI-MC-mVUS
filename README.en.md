[![ModernUI-MC-mVUS](https://cdn.modrinth.com/data/cached_images/fde3425a102171a17ddb69b82538b095ef84ebc8.png)](https://modrinth.com/mod/modernui-mc-mvus)

# Modern UI for Minecraft — mVUS

English | [中文](README.md)

[![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)

> A maintenance fork of **Modern UI for Minecraft** that keeps the mod building and running on minor/patch Minecraft versions, while upstream focuses on stable/LTS releases.

## Overview
**Modern UI for Minecraft** (aka **ModernUI-MC**) is a Minecraft mod built on top of the
[ModernUI Framework](https://github.com/BloCamLimb/ModernUI) (a cross-platform framework for rich desktop UIs on the JDK)
and its extensions.

This project brings ModernUI into the Minecraft environment as a bootstrap/runtime layer, allowing ModernUI-based
applications to run *natively* in Minecraft. It also provides a modding API for **Forge / NeoForge / Fabric** so you can
build feature-rich interfaces.

You can think of this project as a bridge/service layer (plus Minecraft-specific improvements and extensions).
Applications (or program fragments) built with ModernUI can run independently, or inside Minecraft via this mod with no
code changes. Vanilla-based mods can also benefit from the enhanced text system and UI utilities.

If you develop mods with complex interfaces, consider building on the **ModernUI framework** directly (instead of only
adding ModernUI-MC as a runtime dependency). Hybrid UIs are also possible: part ModernUI, part vanilla GUI (with
ModernUI-MC enhancements optionally disabled).

## Highlights
### Modern Text Engine (for vanilla + mods)
ModernUI-MC includes a powerful text layout and rendering system designed for Minecraft. It lets Minecraft and mods that
use the vanilla GUI system benefit from modern Unicode-capable text rendering without code changes:

- Real-time preview and reload of TrueType/OpenType fonts
- Better font fallback
- Anti-aliased text and FreeType hinting
- Improved SDF text rendering in 2D/3D (with batch rendering)
- Exact device-space font sizing for native glyph rendering
- Google Noto Color Emoji (Unicode 16.0 Emoji)
- Configurable bidirectional text heuristics
- Configurable text shadow and raw font size
- Unicode line breaking + CSS `line-break` / `word-break`
- Fast, exact, async text layout computation
- More memory-efficient glyph rectangle packing
- Real alpha mask texture (1 byte-per-pixel; vanilla uses 4 bpp)
- Many optimizations for GUI/sign text rendering (higher FPS, fewer allocations, lower GC pressure)
- Compatibility with OptiFine, Sodium (Rubidium), Iris (Oculus), and many mods
- Compatibility with Minecraft JSON font definitions (bitmap + TTF)

### Quality-of-life utilities
This mod also includes utilities that improve performance and day-to-day UX, such as:

- Screen background color / blur / fade-in animation
- More window modes: borderless fullscreen and borderless maximized
- Framerate limit + master volume fading when unfocused/minimized
- Pause single-player when Inventory is open
- GUI scale slider with hint text
- Startup “ding” sound on reaching the main menu
- Smooth scrolling in vanilla selection lists and Forge scroll panels
- “C” to zoom (OptiFine-style)
- Undo/redo + Unicode word iterator for text fields
- Local music player (seeking + spectrum view)
- Emoji shortcodes in chat (Discord/Slack/GitHub/IamCal/JoyPixels)
- Enhanced tooltip style (rounded borders, title spacing, RTL, pixel-snapped positioning, animated colors)

## Downloads & Community
- Downloads: [Modrinth](https://modrinth.com/mod/modernui-mc-mvus)
- Issues (this fork): [Tealeste/ModernUI-MC-mVUS](https://github.com/Tealeste/ModernUI-MC-mVUS/issues)

## Compatibility
- Minecraft:
  - `26.1-snapshot-1` (**Java Edition 26.1 Snapshot 1**) — Fabric
  - `1.21.9`, `1.21.10`, `1.21.11` — Fabric / Forge / NeoForge
  - Note: The project already supports Minecraft `26.1` snapshots, but the `forgeconfigapiport` mod dependency does not currently support them. Therefore, downloadable builds are currently only available for Minecraft `1.21.9` through `1.21.11` (inclusive).
- Java:
  - `25` for `26.1-snapshot-1` (Gradle must run on Java 25)
  - `21` for `1.21.9`–`1.21.11`
- Build target: defaults to `minecraft_version=26.1-snapshot-1` (override with `-Pminecraft_version=<version>`)

## For Mod Developers
### Gradle repositories
If you are developing against ModernUI-MC, use the IzzelAliz Maven repository. The Maven-published artifacts are not the
same as the CurseForge/Modrinth releases.

```groovy
repositories {
    maven {
        name 'IzzelAliz Maven'
        url 'https://maven.izzel.io/releases/'
    }
    // If you are on Fabric, uncomment this.
    /*maven {
        url 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/'
        content {
            includeGroup 'fuzs.forgeconfigapiport'
        }
    }*/
}
```

### Dependencies
#### Architectury Loom / Fabric Loom
```groovy
dependencies {
  // If you are on Fabric, uncomment this and find a compatible FCAPI version.
  // modApi "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${fcapi_version}"
  implementation "icyllis.modernui:ModernUI-Core:${modernui_version}"
  // Modern UI core extensions
  // Markdown (<=3.11.1) / Markflow (>=3.12.0) is required, others are optional
  implementation "icyllis.modernui:ModernUI-Markflow:${modernui_version}"
  // Choose one of Fabric or NeoForge
  modImplementation("icyllis.modernui:ModernUI-Fabric:${minecraft_version}-${modernui_version}.+")
}
```

#### ModDevGradle (NeoForge)
```groovy
dependencies {
  // Modern UI
  implementation("icyllis.modernui:ModernUI-NeoForge:${minecraft_version}-${modernui_version}.+")
  additionalRuntimeClasspath(compileOnly("icyllis.modernui:ModernUI-Core:${modernui_version}")) {
    exclude group: "org.slf4j", module: "slf4j-api"
    exclude group: "org.apache.logging.log4j", module: "log4j-core"
    exclude group: "org.apache.logging.log4j", module: "log4j-api"
    exclude group: "com.google.code.findbugs", module: "jsr305"
    exclude group: "org.jetbrains", module: "annotations"
    exclude group: "com.ibm.icu", module: "icu4j"
    exclude group: "it.unimi.dsi", module: "fastutil"
  }
  // Modern UI core extensions
  // Markdown (<=3.11.1) / Markflow (>=3.12.0) is required, others are optional
  additionalRuntimeClasspath(compileOnly("icyllis.modernui:ModernUI-Markflow:${modernui_version}")) {
    exclude group: "org.slf4j", module: "slf4j-api"
    exclude group: "org.apache.logging.log4j", module: "log4j-core"
    exclude group: "org.apache.logging.log4j", module: "log4j-api"
    exclude group: "com.google.code.findbugs", module: "jsr305"
    exclude group: "org.jetbrains", module: "annotations"
    exclude group: "com.ibm.icu", module: "icu4j"
    exclude group: "it.unimi.dsi", module: "fastutil"
  }
}
```

#### ForgeGradle 5 (Forge)
```groovy
configurations {
    library
    implementation.extendsFrom library
}
minecraft.runs.all {
  lazyToken('minecraft_classpath') {
    configurations.library.copyRecursive().resolve().collect { it.absolutePath }.join(File.pathSeparator)
  }
}
// Add this block if you have not MixinGradle (https://github.com/SpongePowered/MixinGradle):
minecraft {
  runs {
    client {
      property 'mixin.env.remapRefMap', 'true'
      property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
    }
    server {
      property 'mixin.env.remapRefMap', 'true'
      property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
    }
    // apply to data if you have datagen
  }
  // You need to regenerate run configurations if you make any changes on this.
}
dependencies {
    library("icyllis.modernui:ModernUI-Core:${modernui_version}") {
      exclude group: "org.slf4j", module: "slf4j-api"
      exclude group: "org.apache.logging.log4j", module: "log4j-core"
      exclude group: "org.apache.logging.log4j", module: "log4j-api"
      exclude group: "com.google.code.findbugs", module: "jsr305"
      exclude group: "org.jetbrains", module: "annotations"
      exclude group: "com.ibm.icu", module: "icu4j"
      exclude group: "it.unimi.dsi", module: "fastutil"
    }
    // Modern UI core extensions
    // Markdown (<=3.11.1) / Markflow (>=3.12.0) is required, others are optional
    library("icyllis.modernui:ModernUI-Markflow:${modernui_version}") {
      exclude group: "org.slf4j", module: "slf4j-api"
      exclude group: "org.apache.logging.log4j", module: "log4j-core"
      exclude group: "org.apache.logging.log4j", module: "log4j-api"
      exclude group: "com.google.code.findbugs", module: "jsr305"
      exclude group: "org.jetbrains", module: "annotations"
      exclude group: "com.ibm.icu", module: "icu4j"
      exclude group: "it.unimi.dsi", module: "fastutil"
    }
    // Modern UI for Minecraft Forge
    implementation fg.deobf("icyllis.modernui:ModernUI-Forge:${minecraft_version}-${modernui_version}.+")
}
```

## Building from Source
ModernUI-MC uses a composite build when `../ModernUI` exists (see `settings.gradle`). If you're doing framework
development, clone `ModernUI` next to this repository and keep both up to date.

- Requirements:
  - Snapshot target (`minecraft_version=26.1-snapshot-1`): JDK `25` (Gradle must run on Java 25)
  - Legacy targets (`1.21.9`–`1.21.11`): JDK `21`
  - Check which Java Gradle is using: `./gradlew --version` (see `Launcher JVM`)
  - If needed, set `JAVA_HOME` when invoking Gradle (it must be a JDK home directory, not `/usr/bin/java`)
  - macOS examples:
    - Snapshot (JDK 25): `JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew buildReleaseJars`
    - Legacy (JDK 21): `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew buildReleaseJars -Pminecraft_version=1.21.11`
- One-command distributable jars:
  - Fabric: `./gradlew buildReleaseJars -Pminecraft_version=<mc_version>` (default: `26.1-snapshot-1`)
  - Forge / NeoForge: use `-Pminecraft_version=1.21.11` (26.1 snapshot profiles are Fabric-only currently)
- Output jars (ready for distribution): `build/release/ModernUI-MC-<mod_version>-{fabric,forge,neoforge}.jar`
- Run a dev client:
  - Fabric: `./gradlew :ModernUI-Fabric:runClient`
  - Forge: `./gradlew :ModernUI-Forge:runClient`
  - NeoForge: `./gradlew :ModernUI-NeoForge:runClient`

When you build ModernUI-MC, the universal jar will contain not only ModernUI-MC itself, but also shadow the ModernUI
framework and extensions (except Kotlin extensions), plus additional assets/runtime as needed.

## Changelog
Release notes live in [changelogs.md](changelogs.md).

## License
- Modern UI for Minecraft
  - Copyright (C) 2019-2025 BloCamLimb et al.
  - [![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
- Additional assets
  - [source-han-sans](https://github.com/adobe-fonts/source-han-sans) by Adobe, licensed under OFL-1.1
  - [jetbrains-mono](https://www.jetbrains.com/lp/mono/) by JetBrains, licensed under OFL-1.1
  - [inter](https://github.com/rsms/inter) by RSMS, licensed under OFL-1.1
    - Modern UI for Minecraft contains a modified version of the Inter font family; the original license and
      copyright notice are retained in the font file.

## Screenshots
<details>
<summary>Outdated screenshots (see CurseForge/Modrinth or try in-game)</summary>

Center Screen  
![Center Screen](https://cdn.modrinth.com/data/3sjzyvGR/images/2571f7372b1f9bbb116c118f29a93255338f4e41.png)

New Tooltip  
![New Tooltip](https://s2.loli.net/2024/03/30/VhyoFPAD2Js1HWO.png)

Markdown  
![Markdown](https://cdn.modrinth.com/data/3sjzyvGR/images/989a77ba61c62ff580a30dcf158e391080b949bd.png)

Enhanced Texts for Minecraft Vanilla  
![Enhanced Texts](https://cdn.modrinth.com/data/3sjzyvGR/images/d27f5d77555fd3f45392f5b8eb28efcb80f0b677.png)
![Enhanced Texts (old)](https://s2.loli.net/2022/03/06/TM5dVKnpqNvDiJH.png)

Navigation  
![Navigation](https://s2.loli.net/2022/03/06/hwAoHTgZNWBvEdq.png)

Graphics  
![Graphics](https://i.loli.net/2021/09/27/yNsL98XtpKP7UVA.gif)

Audio visualization  
![Audio visualization](https://i.loli.net/2021/09/24/TJjyzd6oOf5pPcq.png)

Out-of-date widgets  
![Widget (a)](https://i.loli.net/2020/05/15/fYAow29d4JtqaGu.png)
![Widget (b)](https://i.loli.net/2020/04/10/LDBFc1qo5wtnS8u.png)

</details>
