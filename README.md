[![ModernUI-MC-mVUS](https://cdn.modrinth.com/data/cached_images/fde3425a102171a17ddb69b82538b095ef84ebc8.png)](https://modrinth.com/mod/modernui-mc-mvus)

# Modern UI for Minecraft — mVUS

[English](README.en.md) | 中文

[![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)

> 这是 **Modern UI for Minecraft** 的非官方维护分支，致力于在 Minecraft 的次要/补丁版本中保持持续可用；上游项目则更专注于稳定/LTS 版本。

## 概述
**Modern UI for Minecraft**（又称 **ModernUI-MC**）是一个基于
[ModernUI Framework](https://github.com/BloCamLimb/ModernUI)（面向 JDK 的跨平台桌面 UI 框架）及其扩展的 Minecraft 模组。

本项目在 Minecraft 环境中提供 ModernUI 的启动/运行时集成，使基于 ModernUI 的应用能够在 Minecraft 中*原生*运行。同时，它也为
**Forge / NeoForge / Fabric** 提供模组开发 API，便于构建功能完善的图形界面。

你可以将本项目理解为一层桥接/服务层（并附带针对 Minecraft 的改进与扩展）：使用 ModernUI 开发的应用（或局部 UI 片段）既可独立运行，
也可通过本模组在 Minecraft 中运行，通常无需改动代码。基于原版 GUI 的其它模组，也能从更强的文本系统与一系列 UI 工具中受益。

若你在开发具有复杂交互与丰富界面的模组，建议直接以 **ModernUI Framework** 作为开发基础（而不仅仅是把 ModernUI-MC 当作运行时依赖）。
当然也可以做混合 UI：一部分使用 ModernUI，一部分沿用原版 GUI（ModernUI-MC 对原版的增强也可按需关闭）。

## 亮点
### Modern Text Engine（原版与模组通用）
ModernUI-MC 内置为 Minecraft 量身打造的强大文本布局与渲染系统。它让 Minecraft 与依赖原版 GUI 系统的模组在不修改代码的前提下，
也能享受更现代、对 Unicode 更友好的文本渲染能力：

- TrueType/OpenType 字体的实时预览与热重载
- 更完善的字体回退（fallback）
- 抗锯齿文本渲染与 FreeType 字体微调（hinting）
- 改进的 SDF 文本渲染（2D/3D，支持批量渲染）
- 面向原生字形渲染的精确设备空间字号计算
- 内置 Google Noto Color Emoji（支持 Unicode 16.0 表情符号）
- 可配置的双向文本（BiDi）启发式算法
- 可配置的文字阴影与原始字号
- Unicode 断行 + CSS `line-break` / `word-break`
- 高速、精确且异步的文本布局计算
- 更省内存的字形矩形打包算法
- 使用真实 Alpha 遮罩纹理（1 字节/像素；原版为 4 bpp）
- 对 GUI/告示牌文本渲染做了大量优化（更高 FPS、更少分配、更低 GC 压力）
- 兼容 OptiFine、Sodium（Rubidium）、Iris（Oculus）及大量模组
- 兼容 Minecraft 的 JSON 字体定义（位图字体、TTF 字体）

### 体验与性能增强
本模组还提供了多项提升性能与日常体验的实用功能，例如：

- 屏幕背景色/模糊/淡入动画
- 更多窗口模式：无边框全屏、无边框最大化
- 失焦/最小化时的帧率限制与主音量渐隐
- 打开背包时自动暂停单人游戏
- GUI 缩放改为滑块并提供提示文本
- 进入主菜单时播放“叮”提示音
- 原版选择列表与 Forge 滚动面板的平滑滚动
- 按 `C` 缩放（OptiFine 风格）
- 所有文本输入框支持撤销/重做与 Unicode 单词迭代
- 本地音乐播放器（支持拖动进度与频谱显示）
- 聊天中支持 Emoji 短码（Discord/Slack/GitHub/IamCal/JoyPixels）
- 更精致的提示框样式（圆角、标题间距、RTL、像素对齐定位、动画配色）

## 下载与社区
- 下载： [Modrinth](https://modrinth.com/mod/modernui-mc-mvus)
- 问题反馈（本分支） [Tealeste/ModernUI](https://github.com/Tealeste/ModernUI-MC-mVUS/issues)

## 兼容性
- Minecraft：
  - `26.1-snapshot-1`（**Java Edition 26.1 Snapshot 1**）— Fabric
  - `1.21.9`、`1.21.10`、`1.21.11`（Fabric / Forge / NeoForge）
  - 说明：本项目已支持 Minecraft `26.1` 快照，但依赖模组 `forgeconfigapiport` 目前尚不支持；因此当前可下载构建仅提供 Minecraft `1.21.11～1.21.9`（含端点，即 `1.21.9`–`1.21.11`）。
- Java：
  - `25` 用于 `26.1-snapshot-1`（Gradle 必须运行在 Java 25）
  - `21` 用于 `1.21.9`–`1.21.11`
- 构建目标：默认 `minecraft_version=26.1-snapshot-1`（可用 `-Pminecraft_version=<version>` 覆盖）

## 面向模组开发者
### Gradle 仓库
若你需要在开发环境中依赖 ModernUI-MC，请使用 IzzelAliz Maven 仓库。发布到 Maven 的产物与 CurseForge/Modrinth 的发布包并不相同。

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

### 依赖配置
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

#### ModDevGradle（NeoForge）
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

#### ForgeGradle 5（Forge）
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

## 从源码构建
当检测到 `../ModernUI` 存在时，ModernUI-MC 会使用 composite build（见 `settings.gradle`）。若你需要进行框架开发，
请将 `ModernUI` 仓库克隆到本仓库同级目录，并保持二者同步更新。

- 环境要求：
  - 快照目标（`minecraft_version=26.1-snapshot-1`）：JDK `25`（Gradle 必须运行在 Java 25）
  - 旧版本目标（`1.21.9`–`1.21.11`）：JDK `21`
  - 查看 Gradle 使用的 Java：`./gradlew --version`（见 `Launcher JVM`）
  - 如有需要，请在运行 Gradle 时设置 `JAVA_HOME`（必须指向 JDK 目录，而不是 `/usr/bin/java`）
  - macOS 示例：
    - 快照（JDK 25）：`JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew buildReleaseJars`
    - 旧版本（JDK 21）：`JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew buildReleaseJars -Pminecraft_version=1.21.11`
- 一条命令构建可分发 jar：
  - Fabric：`./gradlew buildReleaseJars -Pminecraft_version=<mc_version>`（默认：`26.1-snapshot-1`）
  - Forge / NeoForge：使用 `-Pminecraft_version=1.21.11`（26.1 快照目前仅提供 Fabric 配置）
- 输出位置（可直接用于发布）：`build/release/ModernUI-MC-<mod_version>-{fabric,forge,neoforge}.jar`
- 运行开发客户端：
  - Fabric：`./gradlew :ModernUI-Fabric:runClient`
  - Forge：`./gradlew :ModernUI-Forge:runClient`
  - NeoForge：`./gradlew :ModernUI-NeoForge:runClient`

构建 ModernUI-MC 时，通用 jar 不仅包含 ModernUI-MC 本体，也会将 ModernUI 框架及其扩展（不含 Kotlin 扩展）进行 shadow，
并打包额外资源/运行时依赖（视构建配置而定）。

## 更新日志
更新日志位于 [changelogs.md](changelogs.md)。

## 许可
- Modern UI for Minecraft
  - Copyright (C) 2019-2025 BloCamLimb et al.
  - [![License](https://img.shields.io/badge/License-LGPL--3.0--or--later-blue.svg?style=flat-square)](https://www.gnu.org/licenses/lgpl-3.0.en.html)
- 附加资源
  - [source-han-sans](https://github.com/adobe-fonts/source-han-sans) by Adobe，OFL-1.1
  - [jetbrains-mono](https://www.jetbrains.com/lp/mono/) by JetBrains，OFL-1.1
  - [inter](https://github.com/rsms/inter) by RSMS，OFL-1.1
    - Modern UI for Minecraft 包含经修改的 Inter 字体；原始许可与版权声明已保留在字体文件中。

## 截图
<details>
<summary>过时截图（请以 CurseForge/Modrinth 或游戏内实际效果为准）</summary>

中心界面  
![中心界面](https://cdn.modrinth.com/data/3sjzyvGR/images/2571f7372b1f9bbb116c118f29a93255338f4e41.png)

新提示框  
![新提示框](https://s2.loli.net/2024/03/30/VhyoFPAD2Js1HWO.png)

Markdown  
![Markdown](https://cdn.modrinth.com/data/3sjzyvGR/images/989a77ba61c62ff580a30dcf158e391080b949bd.png)

增强的原版文本渲染  
![增强文本](https://cdn.modrinth.com/data/3sjzyvGR/images/d27f5d77555fd3f45392f5b8eb28efcb80f0b677.png)
![增强文本（旧）](https://s2.loli.net/2022/03/06/TM5dVKnpqNvDiJH.png)

导航  
![导航](https://s2.loli.net/2022/03/06/hwAoHTgZNWBvEdq.png)

图形效果  
![图形效果](https://i.loli.net/2021/09/27/yNsL98XtpKP7UVA.gif)

音频可视化  
![音频可视化](https://i.loli.net/2021/09/24/TJjyzd6oOf5pPcq.png)

旧版控件  
![控件（a）](https://i.loli.net/2020/05/15/fYAow29d4JtqaGu.png)
![控件（b）](https://i.loli.net/2020/04/10/LDBFc1qo5wtnS8u.png)

</details>
