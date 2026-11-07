# MMCE Complement

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62b47a)](https://www.minecraft.net/)
[![Version](https://img.shields.io/badge/version-1.6.2-blue)](https://github.com/Greediest-Studio/MMCE-Complement/releases)
[![Wiki](https://img.shields.io/badge/docs-GitHub%20Wiki-8250df)](https://github.com/Greediest-Studio/MMCE-Complement/wiki)

MMCE Complement is an add-on for [Modular Machinery Community Edition](https://www.curseforge.com/minecraft/mc-mods/modular-machinery-community-edition) on Minecraft 1.12.2. It adds attachment-module multiblocks, advanced machine hatches, compact item/fluid assemblies, recipe conditions, and optional AE2, Flux Networks, Mekanism, and CrazyAE integrations.

MMCE Complement 是 Minecraft 1.12.2 的 MMCE 附属模组，面向整合包作者提供附属模块结构、高阶仓室、物品/流体总成、配方条件及多种网络兼容能力。

## Documentation / 文档

- [AE2S / Supergiant：CR 0.6.8+ 安装、构建与验收](docs/AE2S-COMPATIBILITY.md)
- [存储控制器 CraftTweaker API](docs/storage-controller-crafttweaker-zh_cn.md)
- [GitHub Wiki（完整中文文档）](https://github.com/Greediest-Studio/MMCE-Complement/wiki)
- [Wiki — 中文总览](docs/wiki-zh_cn.md)
- [Wiki — English overview](docs/wiki-en_us.md)
- [Attachment Modules — English](docs/attachment-modules-en_us.md)
- [附属模块机制 — 中文](docs/attachment-modules-zh_cn.md)
- [CurseForge project page](https://www.curseforge.com/minecraft/mc-mods/modularmachinery-community-edition-complement)
- [Issue tracker](https://github.com/Greediest-Studio/MMCE-Complement/issues)

## Highlights

- Attachment modules with dependencies, conflicts, JEI/HEI pages, parent-chain previews, and a structure export tool.
- CraftTweaker module conditions and controller queries.
- Thread, overclock, accelerator, and batch hatches.
- Quadruple and ninefold fluid input/output hatches with optional Mekanism gas support.
- Data input, input, output, and self-cycle assemblies.
- ME ore-dictionary input bus with expression filters and active pulling.
- ME item/fluid/gas inventory buses with fixed markers, active pulling, and permanent stock reserves.
- Mixed ME input, inventory input, output, and full-exposure assemblies.
- ME Channel Input Hatch with dynamic recipe-time channel reservations and JEI support.
- Tier-one ME Data Pattern Provider with per-pattern smart-interface values.
- Tier-two ME Data Pattern Provider II with 144 pattern slots and per-pattern smart-interface values.
- ME Machinery Pattern Provider II with 144 pattern slots and MMCE/Whimcraft compatibility.
- Named redstone signal input/output interfaces and a configurable Redstone Control Hatch.
- Wireless Flux hatches default to a 10,000 FE buffer and 800,000 FE/t transfer rate.
- Configurable liquid energizer and high-capacity filtered output hatches.
- Optional Flux Networks, CrazyAE, and Modular Magic components.

Exact registry names, capacities, configuration keys, and examples are maintained in the [English Wiki overview](docs/wiki-en_us.md) and [中文 Wiki 总览](docs/wiki-zh_cn.md).

## Requirements

- Minecraft 1.12.2
- Minecraft Forge
- Modular Machinery Community Edition
- AE2 Extended Life or Applied Energistics 2 - Supergiant (AE2S)

本分支的 AE2S 目标为 **Cleanroom 0.6.8+ / Java 25**（官方基线标签为 `0.6.8-alpha`）。
使用官方 AppliedCompatibility 1.0.1，兼容修改集中在 MMCE Complement。
准确依赖、实测版本和支持范围见 [AE2S 兼容说明](docs/AE2S-COMPATIBILITY.md)。
保留原项目名称、模组 ID 和作者信息。
已融入 Butterfly-Kiyo 的 PR #1，并补充 MekEng 气体与混合总成的 AE2S 适配；
MMCE 2.3.3 JAR 的验收限制见兼容说明。

MMCE Addons, The One Probe CE, GeckoLib, Flux Networks, CrazyAE, Mekanism, Mekanism Energistics, AE2 Fluid Crafting Rework, Botania, Baubles and display/script integrations are optional to this addon. Their related modules register only when the corresponding mod and API are present. MMCE's bundled Modular Magic API is detected by class availability rather than as a separate Forge mod. See the [installation and compatibility guide](docs/wiki-en_us.md) before distributing a modpack.

## Building

This project uses Gradle with RetroFuturaGradle. From the repository root:

For this branch's AE2S dependencies and Java toolchains, follow the
[AE2S build instructions](docs/AE2S-COMPATIBILITY.md#源码构建).

```powershell
./gradlew build
```

The reobfuscated release jar is written to `build/libs/`.

## License

See [LICENSE](LICENSE) and [third-party source notices](THIRD_PARTY_NOTICES.md).
