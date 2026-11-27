<div align="center">

# MacroMag OS for Android

**Lightweight, Native ARM64 Linux CLI & VOS Environment for Android — Zero Root Required**

[![Build Status](https://img.shields.io/github/actions/workflow/status/MacroMag-os/macromag-android/build-apk.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=APK%20Build)](https://github.com/MacroMag-os/macromag-android/actions)
[![Latest Release](https://img.shields.io/github/v/release/MacroMag-os/macromag-android?style=for-the-badge&logo=android&logoColor=white&color=3FB950)](https://github.com/MacroMag-os/macromag-android/releases/latest)
[![Architecture](https://img.shields.io/badge/Arch-ARM64%20%2F%20aarch64-58A6FF?style=for-the-badge&logo=arm&logoColor=white)](#)
[![License](https://img.shields.io/github/license/MacroMag-os/macromag-android?style=for-the-badge&color=8B949E)](LICENSE)

[Download APK](https://github.com/MacroMag-os/macromag-android/releases/latest) •
[Core OS (`MacroMag-32`)](https://github.com/MacroMag-os/MacroMag-32) •
[Package Repository (`MUR`)](https://github.com/MacroMag-os/mur) •
[Privacy Policy](PRIVACY.md)

</div>

---

## Overview

**MacroMag OS for Android** brings the speed and simplicity of [MacroMag OS](https://github.com/MacroMag-os/MacroMag-32) directly to your Android smartphone or tablet. Designed as a sleek, zero-configuration mobile terminal, it runs the native **ARM64 (`aarch64`)** userland and the **VOS shell** at full native CPU speed without requiring root access or heavy hardware emulation.

---

## Key Features

* **Instant Zero-Config Setup:** Automatically extracts and initializes the MacroMag OS `arm64` root filesystem on first launch.
* **Native ARM64 Performance:** Executes static binaries directly on your device's ARM64 processor without QEMU CPU translation overhead.
* **Built-in MUR Package Manager:** Seamlessly sync and install 50+ statically linked CLI tools (`git`, `eza`, `ripgrep`, `fd`, `fzf`, `bat`, `jq`, `yq`, `busybox`, `age`, `fastfetch`, `dropbear`, and more) directly from the [MacroMag User Repository (MUR)](https://github.com/MacroMag-os/mur).
* **Mobile-First Terminal UI:** Dark terminal theme (`#0D1117`) paired with quick-action shortcut buttons (`sys.cpu()`, `disk.list()`, `mur sync`, `mur list`, `ls -la`, `Ctrl+C`) tailored for touchscreen workflows.
* **100% Offline & Private:** Zero telemetry, zero analytics, and zero background tracking. Everything runs locally on your device.

---

## Installation

### Option 1: Direct Download (GitHub Releases)
1. Open the [Releases page](https://github.com/MacroMag-os/macromag-android/releases/latest) on your Android device.
2. Download the latest signed **`MacroMag-OS-v1.0.1-release.apk`**.
3. Open the downloaded `.apk` file and allow installation from your browser/file manager.
4. Launch **MacroMag OS** — the root filesystem will initialize automatically in a few seconds.

### Option 2: Automatic Updates via Obtainium
Track and auto-update MacroMag OS directly from GitHub Releases using [Obtainium](https://github.com/ImranR98/Obtainium):
* **App Source URL:** `https://github.com/MacroMag-os/macromag-android`

---

## Quick Start Commands

Once inside the **MacroMag OS** terminal, try running:

```bash
# Check CPU & hardware information via VOS
sys.cpu()

# List storage devices and mount points
disk.list()

# Synchronize the MUR package index
mur sync

# List all available ARM64 static packages
mur list

# Install modern CLI tools
mur install eza fastfetch ripgrep busybox
```

---

## Architecture & Ecosystem

MacroMag OS is built as a modular, three-part open-source ecosystem:

| Repository | Role | Description |
| :--- | :--- | :--- |
| **[`MacroMag-os/MacroMag-32`](https://github.com/MacroMag-os/MacroMag-32)** | **Core OS & VOS** | Linux kernel configuration, Rust-based `vos` shell/init, and hybrid ISO builders (`x86_64` & `aarch64`). |
| **[`MacroMag-os/mur`](https://github.com/MacroMag-os/mur)** | **Package Index (`MUR`)** | Declarative `mur.pkg` manifests and automated CI/CD static binary builds for `x86_64` and `arm64`. |
| **[`MacroMag-os/macromag-android`](https://github.com/MacroMag-os/macromag-android)** | **Android Client** | Native Kotlin wrapper, adaptive launcher icon, and automated release APK build pipeline. |

---

## Building from Source

The APK is automatically built and signed via GitHub Actions on every push to `main`. To build locally:

```bash
git clone https://github.com/MacroMag-os/macromag-android.git
cd macromag-android
gradle assembleDebug
```

---

## Privacy & License

* **Privacy Policy:** MacroMag OS collects no user data. Read the full policy in [PRIVACY.md](PRIVACY.md).
* **License:** Distributed under the terms specified in the [LICENSE](LICENSE) file.



