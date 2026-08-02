# MMCE Complement

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62b47a)](https://www.minecraft.net/)
[![Version](https://img.shields.io/badge/version-1.3.1-blue)](https://github.com/Greediest-Studio/MMCE-Complement/releases)
[![Wiki](https://img.shields.io/badge/docs-GitHub%20Wiki-8250df)](https://github.com/Greediest-Studio/MMCE-Complement/wiki)

MMCE Complement is an add-on for [Modular Machinery Community Edition](https://www.curseforge.com/minecraft/mc-mods/modular-machinery-community-edition) on Minecraft 1.12.2. It adds attachment-module multiblocks, advanced machine hatches, compact item/fluid assemblies, recipe conditions, and optional AE2, Flux Networks, Mekanism, and CrazyAE integrations.

MMCE Complement 是 Minecraft 1.12.2 的 MMCE 附属模组，面向整合包作者提供附属模块结构、高阶仓室、物品/流体总成、配方条件及多种网络兼容能力。

## Documentation / 文档

- [GitHub Wiki（完整中文文档）](https://github.com/Greediest-Studio/MMCE-Complement/wiki)
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
- Configurable liquid energizer and high-capacity filtered output hatches.
- Optional Flux Networks, AE2 Extended Life, CrazyAE, and Modular Magic components.

Exact registry names, capacities, configuration keys, and examples are maintained in the [Wiki](https://github.com/Greediest-Studio/MMCE-Complement/wiki).

## Requirements

- Minecraft 1.12.2
- Minecraft Forge
- Modular Machinery Community Edition
- GeckoLib 3

Network and gas integrations register only when their corresponding optional dependencies are present. See the [installation and compatibility guide](https://github.com/Greediest-Studio/MMCE-Complement/wiki/Getting-Started) before distributing a modpack.

## Building

This project uses Gradle with RetroFuturaGradle. From the repository root:

```powershell
./gradlew build
```

The reobfuscated release jar is written to `build/libs/`.

## License

See [LICENSE](LICENSE).
