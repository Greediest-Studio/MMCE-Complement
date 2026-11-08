# MMCE Complement：AE2S / Supergiant 兼容

当前合并版本为 **1.6.2**，保留存储总线设置、延迟保存与库存变更通知修复，
以及 `IMachineStorageController` 脚本接口。AE2S 的存储端口使用独立原生实现；
AppliedCompatibility 同时注册新旧 AE ID 时也只注册一次，不会启用 UEL 专用 Mixin。
下文的 1.6.1 安装验收记录及哈希属于线上 PR 的历史产物，不代表合并后的 JAR 已完成游戏内验收。

本次补齐多方块存储端口的 AE2S 原生气体存储：安装 Mekanism 与原生 MekEng
（Applied Mekanistics 1.2.1）后，物品、流体、气体均可直接访问控制器的实际库存。
气体接口支持模拟存取、多槽大总量及共享后端锁；终端通过原生存储总线监视器同步，
外部管道/脚本改动继续由控制器的变更标记触发刷新。未安装原生 MekEng 时不加载气体适配类，
不影响物品、流体与 CraftTweaker 接口。存储交互总成仍为管道容器，不接入 ME 导线。

动态容量方法支持按单台已成型存储器设置三类槽数、流体/气体单槽及批量默认容量，
与 UEL 共用后端、持久化和脚本契约。缩容会拒绝删除非空槽或截断内容；说明见
[存储控制器 CraftTweaker API](storage-controller-crafttweaker-zh_cn.md)。

本次 Gradle 验证包括 UEL 回归、无原生 MekEng 的 AE2S 回归、AE2S 共享脚本测试，
以及 8 项使用真实原生气体键和存储总线监视器的气体测试。
本次 `build reobfRuntimeHarnessJar` 共 203 项测试：202 通过、1 项按原条件跳过。
另以 `test -Ptest_ae_storage_gas --tests '*StorageMEGasInventoryTest'` 单独通过了该可选 UEL 气体测试，
包括既有 UEL 端口读取动态槽数、单槽容量、模拟存入与准确数量通知。
这些测试不替代合并后安装包的游戏内验收。

## 历史 1.6.1 安装与验收记录

以下记录来自合并的上游 PR，版本、哈希和游戏验收均按原产物保留。当前安装包为 1.6.2。

该适配分支基于 YDHusky/MMCE-Complement 的 e293441a08b096f35624b26bdc18d8e60f16b9df，
已检查作者的 AE2S 初步适配提交 f98d33366476c6c9898b68908f8162e971fd3dec。
保留 **MMCE Complement** 名称、mmce_complement ID、net.edwin 包名、版本 1.6.1
及原有作者信息，继续使用原 RetroFuturaGradle 构建。

## 安装

目标为 Minecraft 1.12.2、**Cleanroom 0.6.8+、Java 25**。
0.6.8 的官方完整标签为 0.6.8-alpha；本文以该实际发行包为最低验收版本。

| 组件 | 固定验收版本 | 安装方式 |
| --- | --- | --- |
| MMCE Complement | 1.6.1，本分支产物 | 替换旧 Complement JAR |
| AE2S / Supergiant | 1.1.0+a47dfe029 | 官方独立安装 |
| AppliedCompatibility | **官方 1.0.1，未修改** | 官方独立安装 |
| Modular Machinery Community Edition | 2.3.2，CurseForge 文件 7372951 | 官方独立安装 |
| CraftTweaker2 | 4.1.20.698 | MMCE 前置 |
| GeckoLib3 | 3.0.30，CurseForge 文件 4020277 | MMCE 前置 |
| Had Enough Items | 4.35.1 | 验收时安装，沿用 jei Mod ID |

外部依赖的来源、文件名和 SHA-256 见 [基础依赖锁](../verification/dependencies.json)。
气体组合另见 [气体依赖锁](../verification/dependencies-gas.json)，在上表基础上安装
Mekanism CE Unofficial 10.0.5.222、MekEng 1.2.1、Mekanism CEu AE Upgrade Supergiant 1.3.0。

MMCE **2.3.3** 的监听器兼容修复已融入本分支。已检查官方发行页、CI 和开发分支，
下载到的可验证发布/CI JAR 仍为 2.3.2；另外下载了 Circulate233 的 2.3.3 源码，
核实 `IOInventory.IntConsumer extends Consumer<Integer>` 的 `accept(int)` / `accept(Integer)` 签名，
并用同形接口测试回调。**2.3.3 安装 JAR 未完成运行验收**，不把 2.3.2 报告记作 2.3.3。
来源与检索结果见 [2.3.3 记录](../verification/mmce-2.3.3-sources.json)。
保持 config/appcompat.cfg 的 enableMMCE=true（默认启用）。
同一模组只安装一份；源码 JAR、开发 JAR 和验证 harness 均不应放入正常游戏的 mods 目录。

交付目录为 build/ae2s-official-release/。其中只有 Complement 的安装产物和对应源码；
MMCE、AE2S、AppliedCompatibility 均使用上表的官方 JAR，不嵌入、不重建、不替换其代码。

## Complement 侧的改动

- 使用 AppliedCompatibility 为 MMCE 提供的原生主节点，给 Complement 仓室注册真实调度、
  存储和样板服务。没有额外创建一个并行的仓室主节点。
- 补足官方 1.0.1 安装 JAR 在 SRG 环境遗漏的节点初始化/失效钩子；
  原生节点状态回调直接持久化、同步仓室，避免关闭世界时查询已销毁的旧代理网络。
  这些钩子都放在 Complement 的可选 AE2S Mixin 中。
- 存储端口使用 AE2S 原生物品/流体/气体存储，增量的数量与原型分离，避免官方旧栈将负数量截零
  造成外部库存重复记账。多端口去重、拆除封存、重建和断线接管沿用 Complement 的库存规则。
- 原生 AE2S 编码样板接入 36/144 槽供应器，保留数据标记和工作模式；原生服务支持合并下发。
- Complement 的三种供应器使用原生 AE2S 容器、界面和同步协议，支持扩展槽位、三个独立流体槽、
  工作模式、退回缓存和数据样板编辑。
- 修正数据供应器 II 的安装产物创建路径，确保实际创建数据 Tile，保留末尾槽位的数据标记；
  扩宽编辑弹窗，避免英文标签覆盖输入控件，原生输入总线的长标题限制在窗口内。
- 原生物品/流体输入总线界面保留 Complement 的主动拉取和永久保留量面板；
  矿辞总线保留白名单、黑名单及其配置包的所属容器检查。
- 频道仓室读取实际 AE2S 路径容量，释放预留时销毁私有端点；
  连接共享使用原生网络身份，处理重复收费、冲突网络、断连和结构拆除。
- 修正服务端路径中的客户端翻译类引用与 silver 信号线资源映射。
- 融入 [Butterfly-Kiyo 的 PR #1](https://github.com/Greediest-Studio/MMCE-Complement/pull/1)：
  `9c6a5ac611b65dafa7dc7caa9dc883be1a11315e` 的总成库存监听适配，以及
  `d6687a11b67956a4242bc45cb5116bdb9b883aa9` 的服务端安全物品名翻译。
  保留旧 `onChange()`，补齐 MMCE 自定义 `accept(int)` / `accept(Integer)` 和代理身份方法；
  输入/输出库存修改立即更新显示、标记脏数据并唤醒网络。
- 复现并修复 `GasInventory.getTanks()` 缺失：官方兼容层以 record 的 `tanks()`
  访问数组，原 MMCE 仍调用旧 getter。Complement 的气体专用 Mixin 复用原数组补足 getter。
- 本地 key/monitor 桥补齐气体通道的存储查询、带能量拉取和退回；
  混合总成使用保留原 NBT 布局的流体库存，补齐官方 shim 缺失的构造/容量/流体接口。
- 修复库存型气体总线读档、换升级卡和打开原生 GUI 时被普通总线容量截断的问题，
  保留大缓冲、配置和永久保留量。
- 混合输入、输出、库存输入与全暴露总成使用 AE2S 原生容器和界面；
  混合过滤槽支持 item/fluid/gas，库存过滤量固定为 1，全暴露界面不可设置过滤器。
- 库存总线与总成的主动拉取、永久保留量通过原生容器同步初值及后续更新；
  未编辑保留量时关闭界面不会写回客户端旧值。混合总成的长标题限制为一行。
- 输出总成背景按实际 204 像素高度完整显示，玩家背包从 y=123 开始、快捷栏位于
  y=181，背包标题位于 y=111；AE2S 原生界面和原有界面使用相同布局，修复底部裁切与标题重叠。

原生供应器容器和基础界面改编自 AppliedCompatibility 的 LGPL-3.0-or-later 代码。
对应源文件、修改说明及随包许可证见 [第三方声明](../THIRD_PARTY_NOTICES.md)。

## 验收

使用实际 SRG 安装 JAR、Java 25、官方 MMCE / AE2S / AppliedCompatibility。
结果和实际安装哈希按版本归档在 [验收报告](../verification/reports/README.md)。
最新输出总成 UI 修订使用 CR 0.6.13-alpha 中文客户端与专服验收；
更早的完整版本矩阵保留各自的产物哈希，不能当作最新 JAR 在所有版本上的重跑结果。
官方 **0.6.8-alpha、0.6.13-alpha、0.7.14-alpha** 的结果分别记录，
不能据这三个版本推断所有未来 Cleanroom 版本均已验收。
早期验收脚本强制设置 `-Dfoundation.loadsall=true`，在较新加载器的版本检查处触发
`Restriction.toStringFriendly()` / `ft` 类缺失；去掉该测试选项后使用默认类加载方式重跑。
官方加载器及前置 JAR 保持原样。逐次类加载设置见验收清单，
完整启动命令与原始日志保留在本地 `verification/evidence/`。

服务端检查包括：无气体时 49 类、安装气体时 53 类已注册 TileEntity 的构造与 NBT、原生节点、存储模拟/插入/提取和精确增量、
外部库存编辑、真实调度器拉取及保留量、缓存退回、加工样板、数据标记、最后槽位 35/143、
合并下发、阻挡/增强阻挡/合成锁/隔离输入、频道预留和释放、真实多方块的双端口去重、
拆除与重建、断线接管，以及 Complement 与原 MMCE 仓室连接共享。
气体场景额外检查原 MMCE 气体输入/输出、混合 item/fluid/gas 实际调度与数量守恒，
消耗后补给、清配置退回、库存型气体保留量、大缓冲 NBT 与升级卡变更、
混合库存总成与全暴露总成的三类资源拉取/退回。

客户端检查使用服务端打开的真实容器，覆盖水桶与流体槽同步、工作模式和缓存退回按钮、
物品/流体拉取设置、矿辞设置、数据供应器末尾槽位编辑与客户端/服务端同步。
气体客户端还检查原生气体配置包、18,150 单位库存同步、待机退回，以及混合输入/输出、
库存输入和全暴露界面的槽位/过滤器/保留量面板、初始设置同步及关闭界面后设置保持不变。
仓库收录结果、依赖及产物哈希和输出总成修复截图；完整日志与其余截图保留在本地。
关闭世界时的节点销毁异常也会令启动器判定失败。

既有 Gradle 回归测试共 **151 项：150 通过、1 项按原有条件跳过**。
这些测试含旧 API 环境；不能据此声明原版 AE2 或 AE2UEL 已通过安装产物验收。

### 支持边界

- 支持声明限于报告中的 AE2S 固定依赖组合。AE2UEL 与原版 AE2 未作运行验收。
- 气体支持限于气体依赖锁的固定组合；用户的 Windows / 361 Mod 整包未实际运行。
  2.3.3 的监听 API 已按源码和 PR 适配，完整 2.3.3 JAR 运行仍未验收。
- CrazyAE 能量/魔力和其它未列入报告的可选附属组合未完成原生通道验收。
- 未新增 Ponder 演示。未以独立远程客户端连接专服做额外联机验收。
- 上游 MMCE 的旧 MixinDualityInterface 因官方桥接类没有 iHost 字段而被跳过，日志保留该警告。
  已验收的原生样板与共享网络不依赖该旧混入。
- 客户端有模型加载日志，涉及测试 JSON 控制器、MMCE 的工作中组件模型和九重流体仓尺寸变体。
  气体组合还有 Mekanism 的 `module_laser_cutter_unit`、`module_elytra_unit`、`hdpe_elytra`
  三条配方解析错误。不将功能检查通过描述为日志完全无警告或错误。

## 源码构建

使用仓库 Gradle Wrapper / RetroFuturaGradle 2.0.2，启动 Gradle 的 JAVA_HOME 为 JDK 25。
另需 JDK 8（原始 Forge/Minecraft 源码、旧 Fernflower）和 JDK 21（JST）。
主模组仍输出 Java 8 字节码，AE2S 和官方桥接运行时要求 Java 25。

默认构建会自动下载官方 AE2S 1.1.0、AppliedCompatibility 1.0.1 和 MekEng 1.2.1 的开发包，无需原作者机器的目录：

    ./gradlew build reobfRuntimeHarnessJar --no-daemon

UEL 回归测试运行在 Java 8；独立的 AE2S 测试运行在 Java 25。
`ae2sGasTest` 单独引入原生 MekEng，覆盖真实气体键和终端监视器，
`ae2sTest` 保持无原生 MekEng 环境；两者均随 `check` / `build` 执行。
需要使用本地开发包时，仍可通过以下参数覆盖：

    gradle reobfJar reobfRuntimeHarnessJar sourcesJar test --console=plain \
      -Pae2s_dev_jar=/absolute/path/AppliedEnergistics2-Supergiant-1.1.0+a47dfe029-dev.jar \
      -Pappcompat_dev_jar=/absolute/path/appcompat-1.0.1-dev.jar \
      -Pmekeng_dev_jar=/absolute/path/mekeng-1.2.1-dev.jar \
      -Dorg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk21,/path/to/jdk25 \
      -Dorg.gradle.java.installations.auto-download=false

build/libs/mmce_complement-1.6.2.jar 为安装产物。
build/verification/mmce-complement-verification-srg.jar 为独立验收模组，不属于正常安装包。
构建依赖使用 compileOnly，不把官方前置打入 Complement JAR。

## 重跑验收

将依赖锁中的六个官方 JAR 放在一个目录，构建上述两个安装 JAR，再运行：

    python3 verification/run.py server --name mmce-recheck-server --dependencies /path/to/jars
    DISPLAY=:97 LIBGL_ALWAYS_SOFTWARE=1 python3 verification/run.py client \
      --name mmce-recheck-client --dependencies /path/to/jars
    python3 verification/run.py server --name mmce-recheck-0714 \
      --cleanroom-version 0.7.14-alpha --dependencies /path/to/jars

气体场景使用额外的依赖锁：

    python3 verification/run.py server --name mmce-recheck-gas --dependencies /path/to/gas-jars \
      --dependency-lock verification/dependencies-gas.json

verification/launch.py 按官方安装器中的配方及 SHA-1 提取/校验加载器与运行库，
使用原始 Minecraft 安装 JAR，不使用 userdev 产物。当前启动脚本需要 Linux x64、
已准备的 Minecraft 1.12.2 资源/元数据缓存和有效 X11 显示。可用 AE2_ADDONS_JAVA_HOME 指定 JDK 25。
runtime.json 记录实际版本、安装器和所有 JAR 哈希；result.json 区分启动通过与功能通过。
