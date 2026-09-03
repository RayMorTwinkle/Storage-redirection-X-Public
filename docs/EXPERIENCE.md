# Storage-Redirection-X Fork 经验（改动 · 编译 · 测试 · 发布）

> 版本：`v1.2.56`（`5b7d7ae`，基于 `1.2.55 229 → 1.2.56 230`）  
> 仓库：`RayMorTwinkle/Storage-redirection-X-Public`（fork 自 `Kindness-Kismet/Storage-redirection-X-Public`）  
> 周期：2026-09-02 ～ 2026-09-03  
> 关联技能：`me-android-bridge`、`me-android-cmdline-toolchain-v1`、`me-skill-l0-manager-v1`

---

## 1. 改动

### 目标
为**每个应用**增加 **黑名单 `black` / 白名单 `whitelist`（默认）** 重定向模式，旧配置无损兼容。

- **白名单**：默认隔离全部存储，仅放行 `allowed_real_paths`。
- **黑名单**：默认放行全部公共存储，仅隔离 `!` 前缀的 `excluded` 路径。

### Rust（`srx_core`）

| 文件 | 作用 |
|------|------|
| `srx_core/src/domain.rs:1` | `enum RedirectMode { Whitelist, Blacklist }`，`Default=Whitelist`，`from_str` 识别 `black/blacklist`，`is_blacklist()` |
| `srx_core/src/config.rs` | `UserProfile.mode: RedirectMode` |
| `srx_core/src/config/ingest.rs` | `parse_app_config` 解析 `mode` 字段；`!` 前缀的 `allowed_real_paths` 拆分到 `excluded_real_paths` |
| `srx_core/src/config/inspect.rs` | `get_redirect_mode()` / `get_excluded_real_paths()` |
| `srx_core/src/redirect/router.rs` | `PathRouter::configure()` 接收 `mode`，`process_path()` 黑名单分支：已在 `is_excluded_real_path` 命中则重定向到隔离区，否则放行 |
| `srx_core/src/redirect/engine.rs` | 系统写者（MediaProvider 等）分支支持 `caller_mode.is_blacklist()` |
| `srx_core/src/mount/apply.rs` | `apply_blacklist_redirect()`：仅对黑名单目录在隔离区建目录并 `bind`，不 `chown` 真实公共目录（`false`） |
| `srx_core/src/lifecycle/specialize_pre.rs` | 读取 `redirect_mode` 并传入 `PathRouter` 与 companion payload（`blacklist_mode` + `excluded_real_paths`） |
| `srx_core/src/lifecycle/companion_request.rs` / `companion_mount.rs` | `is_blacklist_mode` / `excluded_real_paths` 透传，分流到 `apply_blacklist_redirect` |
| `assets/zygisk_module/sepolicy.rule` | 放行 `mediaprovider` 读配置 |

### Kotlin（Manager）

| 文件 | 作用 |
|------|------|
| `android/app/src/main/kotlin/com/storage/redirect/x/data/model/AppRedirectConfig.kt` | `enum RedirectMode`，`mode` 字段，`toUserJson` / `fromUserJson` 仅黑名单写 `mode:"black"` |
| `android/app/src/main/kotlin/com/storage/redirect/x/data/model/RedirectConfig.kt` | `setRedirectMode()`，黑名单下路径自动加 `!` 前缀转 excluded |
| `android/app/src/main/kotlin/com/storage/redirect/x/ui/page/RuleConfigPage.kt` | 分段卡片式模式切换（`RedirectModeCard`，`primary/surfaceContainer` + 1dp 边框），标题/占位/提示随模式切换（`禁止访问路径/Disallowed Paths`），展示时去 `!` 前缀 |
| `android/app/src/main/res/values* /strings.xml` | `mode_whitelist_desc / mode_blacklist_desc` 等 4 串中英 |

### 关键语义坑
- **配置目录**：模块实际读 `/data/adb/modules/storage.redirect.x/config`，曾误写 `/dev/srx_config` 导致不生效。
- **`!` 前缀**：Rust 层 `!` → `excluded_real_paths`；Kotlin 层黑名单下用户输入 `Download` 自动补 `!Download`，避免“填了不隔离”。

---

## 2. 编译

### 本机（`me-android-cmdline-toolchain-v1`，无 Studio/无模拟器）

```bash
/opt/homebrew/bin/brew install --cask android-commandlinetools
# SDK 根：/opt/homebrew/share/android-commandlinetools（ln → ~/Library/Android/sdk 兼容 Gradle）
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "platforms;android-35" "platforms;android-36" "build-tools;34.0.0" "build-tools;35.0.0" "build-tools;36.0.0"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug  # 首次 1m18s（拉依赖），增量数秒；产物 app/build/outputs/apk/debug/app-debug.apk（12M）
```

- **compileSdk 坑**：`activity-compose 1.12.4` / `core 1.17.0` 等要求 `compileSdk ≥36`，`platforms;android-37` 在当前 `sdkmanager` 找不到，需升 `compileSdk 34→36`（已改 `app/build.gradle.kts:11`）并装 `platform-36`。
- **JDK**：本机 `openjdk 26.0.2.1` + `Gradle 9.5.1` 实测可过；若 AGP 报 `unsupported Java version` 再切 `openjdk@17`。
- **sdkmanager deprecated**：忽略，仍可用；新 CLI 为 `android sdk`。
- **TextReader 同链路**：`~/Documents/Fork/TextReader` 复用同一 wrapper 与 `libs.versions.toml`（`agp 9.2.0-alpha07 / kotlin 2.3.10 / compose-bom 2026.02.00`），`./gradlew assembleDebug` 同样秒级。

### 云端（GitHub Actions）

- `fork-build.yml`（`main` 手动触发，`arm64-v8a`）用于快速验证；`release.yml`（`v*` tag）构建 `arm64-v8a + x86_64` 并发版。
- v1.2.56：`33614951280` **success 5m54s**（`arm64 4m57s + x86 5m30s + 发布 6s`，`debug 签名`）。

### 对比

|  | 本机 | 云端 |
|---|---|---|
| 适用 | 开发内循环（改 UI/权限需 20 次/天） | 发版/可复现归档 |
| 延迟 | 秒级 | 5–6 分钟 + 排队 |
| 调试 | `logcat / debugger` 直连 | 仅日志 |
| 结论 | 写得爽 | 发得稳 |

---

## 3. 测试

### 探针：TextReader（`com.textreader`，自建）

- 功能：`File("/storage/emulated/0")` 递归扫 6 层、`*.txt/.md/.log/.json...` <5MB、`Material3` 列表 + 预览，`MANAGE_EXTERNAL_STORAGE`（`appops set allow` 后 `force-stop` 生效）。
- 位置：`~/Documents/Fork/TextReader`，`compileSdk 36`，`minSdk 24`。

### 黑名单 A/B（真机 T508N / Android 13 / `T6PA04CJ6EH01DD`）

探针目录：`/sdcard/Download/srx_blacklist_probe/probe_a.txt` + `probe_b.txt`（10B）  
配置：`{"users":{"0":{"enabled":true,"mode":"black","allowed_real_paths":["!Download/srx_blacklist_probe"]}}}` → `/data/adb/modules/storage.redirect.x/config/apps/com.textreader.json`

| 状态 | TextReader 计数 | `nsenter -t <pid> -m -- ls /storage/emulated/0/Download/srx_blacklist_probe` | 真实路径 | mountinfo |
|------|----------------|-----------------------------------------------|----------|-----------|
| A 无配置 | 113 | 可见 2 文件 | 可见 | 无隔离 |
| B 黑名单 | 111（-2） | `total 0`、`cat: No such file` | 可见 | `bind /media/0/Android/data/com.textreader/sdcard/Download/srx_blacklist_probe → /storage/emulated/0/Download/srx_blacklist_probe`（含 `/mnt/*` 别名） |

- **判定**：挂载层隔离 + 应用层计数 + 读取失败均精确 -2，全链路符合预期，无需迭代 Storage。
- **Hook 警告**：`W StorageRedirect: hook install failed nativeGetString / reader hook install failed pkg=com.textreader` 为 `W` 级，不崩溃（StarNote 因 `WorkManager Room found: null` 崩溃属其自身库问题，挂载仍曾成功 `total 0`）。

### 其他验证

- StarNote `1.5.2`（`com.onyx.galaxy.note`，469M）同样验证挂载 `Download total 0`，但应用层崩溃，故切 TextReader 作主探针。
- 通用：白名单/黑名单/无隔离路径、`!Download` 父目录隔离均走同一 `apply_blacklist_redirect` 分支。

---

## 4. 发布

- 版本：`appVersionName 1.2.55 → 1.2.56`，`appVersionCode 229→230`（`android/gradle.properties`）
- 提交：`5b7d7ae chore: bump version to 1.2.56 (blacklist verified via TextReader)`（含 `fe67c2b / ca6f714 / 9ded7a4` 三个未发版提交）
- Tag：`v1.2.56`（annotated）→ `origin/main` + `origin/v1.2.56`
- Release：`Storage Redirection X 1.2.56`（Latest，`2026-09-02T09:41:22Z`，`github-actions[bot]`），4 产物：`_1.2.56_arm64-v8a.apk` / `_x86_64.apk` / `_v1.2.56-arm64-v8a.zip` / `_x86_64.zip`
- CHANGELOG：`.github/CHANGELOG.md` 已含黑名单中英说明。

---

## 5. 工具链沉淀

- `me-android-bridge`：`T6PA04CJ6EH01DD`、`adb forward tcp:7474`、`NeuralBridge MCP 0.4.0`（`com.neuralbridge.companion` `*:7474`，`remote http://127.0.0.1:7474/mcp`，`neuralbridge.*` 32 工具），`ida-pro-mcp` 保留，`mobile-mcp` 已移除。
- `me-android-cmdline-toolchain-v1`：本机无 Studio 编译链完整记录，验证环节引用 `me-android-bridge`。

---

## 6. 后续

- 可选：StarNote 兼容（`nativeGetString` hook）深挖；黑名单嵌套目录挂载顺序优化；MediaProvider 黑名单热重载。
- TextReader：可加“按文件夹分组 / 深色主题 / 编码切换”。

