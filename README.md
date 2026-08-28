# MMCE Complement

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62b47a)](https://www.minecraft.net/)
[![Version](https://img.shields.io/badge/version-1.4.0-blue)](https://github.com/Greediest-Studio/MMCE-Complement/releases)
[![Wiki](https://img.shields.io/badge/docs-GitHub%20Wiki-8250df)](https://github.com/Greediest-Studio/MMCE-Complement/wiki)

MMCE Complement is an add-on for [Modular Machinery Community Edition](https://www.curseforge.com/minecraft/mc-mods/modular-machinery-community-edition) on Minecraft 1.12.2. It adds attachment-module multiblocks, advanced machine hatches, compact item/fluid assemblies, recipe conditions, and optional AE2, Flux Networks, Mekanism, and CrazyAE integrations.

MMCE Complement 是 Minecraft 1.12.2 的 MMCE 附属模组，面向整合包作者提供附属模块结构、高阶仓室、物品/流体总成、配方条件及多种网络兼容能力。

## Documentation / 文档

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
- ME Machinery Pattern Provider II with 144 pattern slots and MMCE/Whimcraft compatibility.
- Named redstone signal input/output interfaces and a configurable Redstone Control Hatch.
- Wireless Flux hatches default to a 10,000 FE buffer and 800,000 FE/t transfer rate.
- Configurable liquid energizer and high-capacity filtered output hatches.
- Optional Flux Networks, AE2 Extended Life, CrazyAE, and Modular Magic components.

Exact registry names, capacities, configuration keys, and examples are maintained in the [English Wiki overview](docs/wiki-en_us.md) and [中文 Wiki 总览](docs/wiki-zh_cn.md).

## Requirements

- Minecraft 1.12.2
- Minecraft Forge
- Modular Machinery Community Edition
- AE2 Extended Life

GeckoLib, Flux Networks, CrazyAE, Mekanism, Mekanism Energistics, Modular Magic, AE2 Fluid Crafting Rework, Botania, Baubles and display/script integrations are optional to this addon. Their related modules register only when the corresponding mod and API are present. See the [installation and compatibility guide](docs/wiki-en_us.md) before distributing a modpack.

## Building

This project uses Gradle with RetroFuturaGradle. From the repository root:

```powershell
./gradlew build
```

The reobfuscated release jar is written to `build/libs/`.

## License

See [LICENSE](LICENSE).
