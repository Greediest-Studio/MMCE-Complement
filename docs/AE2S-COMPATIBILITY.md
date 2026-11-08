# AE2UEL 与 AE2S 兼容

MMCE Complement 支持 AE2 Extended Life（AE2UEL）和 Applied Energistics 2 - Supergiant（AE2S）。两者使用相同的机器 JSON 与 Complement CraftTweaker API，在同一实例中选择一个分支安装。

## 安装组合

| 组件 | AE2UEL | AE2S / Supergiant |
| --- | --- | --- |
| Minecraft | 1.12.2 | 1.12.2 |
| 加载环境 | 与 MMCE 和 UEL 配套的 Forge 环境 | Cleanroom 0.6.8+、Java 25 |
| ME 模组 | AE2 Extended Life | AE2S 1.1.0 系列 |
| MMCE 兼容层 | 使用 MMCE 的对应兼容 API | 官方 AppliedCompatibility 1.0.1，保持 `enableMMCE=true` |
| 仓室气体能力 | Mekanism | Mekanism CE Unofficial |
| ME 气体网络 | 对应 UEL 的 Mekanism Energistics | 原生 MekEng / Applied Mekanistics 1.2.1 |

两种组合均需 MMCE、MixinBooter 及 MMCE 自身要求的 CraftTweaker、GeckoLib 等前置。AE2S 的基础组合可使用 MMCE 2.3.2、CraftTweaker 2 4.1.20.698、GeckoLib 3.0.30；Complement 同时适配 MMCE 2.3.3 的库存监听接口。AE2S 气体组合可使用 Mekanism CEu 10.0.5.222；需要对应升级卡功能时安装 Mekanism CEu AE Upgrade Supergiant 1.3.0。

AppliedCompatibility 在 AE2S 环境中提供旧 AE API 的桥接标识，不需要额外安装 AE2UEL。客户端和服务端须使用相同组合，每个模组只保留一份安装 JAR。

## ME 组件

| 功能 | 使用方式 |
| --- | --- |
| 物品、流体库存输入总线 | 连接有电 ME 网络，在 GUI 设置标记、主动拉取和永久保留量 |
| 气体总线与混合 ME 总成 | 安装该 AE 分支的气体网络扩展，在对应槽位配置气体 |
| 机械样板供应器与数据供应器 | 使用所选 AE 分支的编码样板；二级供应器支持 144 个样板槽 |
| 存储访问端口 | 直接将成型多方块存储器接入 ME，使用原生存储总线 GUI 设置过滤、访问模式和优先级 |
| 频道输入仓与连接共享仓 | 按实际 ME 网络分配频道或共享连接，保留各组件的频道需求 |

ME 存储访问端口、管道交互总成、控制器与脚本读取同一份机器库存。相同存储器通过多个直接端口接入同一 ME 网络时，网络只计入一份库存。

`storage_interaction_assembly` 是无 GUI 的管道容器，不能直接连接 ME 导线。其气体管道能力只需 Mekanism；将气体提供给 ME 时还需要上表对应的 ME 气体扩展。

## 可选网络功能

Flux Networks 仓室和 MMCE Addons 检测仓按各自依赖启用。CrazyAE 的能源、魔力通道需要其专用 API；AE2S 本身不提供这些通道。不要仅通过方块是否出现在注册表中判断某个可选附属的通道是否可用。

## 配置与脚本

机器 JSON、附属模块、红石数据与 `IMachineStorageController` 在两个分支使用相同写法。存储脚本在服务端主线程操作已成型机器，变更会同步到对应分支的 ME 存储。

- [安装与快速开始](https://github.com/Greediest-Studio/MMCE-Complement/wiki/Getting-Started)
- [存储接口与自动化](https://github.com/Greediest-Studio/MMCE-Complement/wiki/Storage-Interfaces)
- [CraftTweaker 支持](https://github.com/Greediest-Studio/MMCE-Complement/wiki/CraftTweaker-API)
- [常见问题](https://github.com/Greediest-Studio/MMCE-Complement/wiki/Troubleshooting)
