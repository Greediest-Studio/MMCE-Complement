# MMCE Complement Wiki（中文）

当前文档对应 MMCE Complement **1.4.5**，运行环境为 Minecraft 1.12.2。

[English Wiki](wiki-en_us.md) · [附属模块指南](attachment-modules-zh_cn.md)

## 项目简介

MMCE Complement 是 [Modular Machinery Community Edition（MMCE）](https://www.curseforge.com/minecraft/mc-mods/modular-machinery-community-edition) 的附属模组，为整合包作者提供更多机器仓室、物品/流体/气体总成、AE2 网络桥接、红石数据接口、附属模块和 CraftTweaker 扩展。

## 安装与可选依赖

必需的内容模组依赖为 Minecraft Forge、MMCE 和 AE2 Extended Life；MixinBooter 仍是必需的技术加载器。其他兼容功能对本附属模组均为可选，只有同时检测到对应模组及所需 API 类时才会注册。特定 MMCE 版本自身仍可能传递依赖 GeckoLib，但 MMCE Complement 不再把它声明为自己的强依赖。

| 功能 | 额外可选依赖 |
| --- | --- |
| ME 物品总线、ME 机械样板供应器 II、ME 频道输入仓 | 无（仅使用必需的 AE2/MMCE） |
| ME 能源输入/输出总线 | CrazyAE |
| ME 魔力输入/输出总线 | CrazyAE + MMCE 内置的 Modular Magic API |
| ME 气体总线和混合气体总成 | Mekanism + Mekanism Energistics |
| 样板供应器 II 的假流体/假气体原料 | AE2 Fluid Crafting Rework（假气体还需 Mekanism Energistics） |
| 无线通量仓 | Flux Networks |
| 配方脚本和控制器脚本 API | CraftTweaker 2 |
| JEI 配方显示 | JEI 或 HEI（由 MMCE 提供的显示环境决定） |
| 星辉魔法配方注册表兼容修复 | Astral Sorcery + Nova Engineering（由相关适配器触发时生效） |

方块的注册 ID 以 `mmce_complement:` 为命名空间。可选依赖未安装时，相应 ID 不会出现在注册表中。

## 基础仓室与总成

### 性能仓

- **线程仓**（`thread_hatch`）：MK1–MK6，提高机器的普通线程上限。默认倍率为 2、3、5、8、12、16；可在配置中调整。默认同一机器只取最高等级，`allow_stacking=true` 时所有线程仓倍率相乘，不会增加特殊自定义线程。
- **超频仓**（`overclock_hatch`）：MK1–MK6，同时提高耗能并缩短配方时间，倍率可配置；默认只取最高等级，也可启用叠加。
- **加速仓**（`accelerator_hatch`）：MK1–MK8，无条件缩短配方时间；同一机器只生效最高等级。
- **批处理仓**（`batch_hatch`）：在 GUI 中设置最大批处理时间，用运行时间换取更高的单批并行数。多个批处理仓同时存在时取设置时间最大的一个。

### 多槽流体仓

- **四重流体输入/输出仓**（`quad_fluid_input_hatch_tiny`、`quad_fluid_output_hatch_tiny`）：沿用 MMCE 的八档容量（微型至真空），每个方块有 4 个相互隔离的储槽。输入仓不允许重复流体，输出仓允许不同槽位存放相同流体。
- **九重流体输入/输出仓**（`nine_fluid_input_hatch_normal`、`nine_fluid_output_hatch_normal`）：中型至真空共 6 档，拥有 9 个隔离储槽；输入仓不允许重复，输出仓允许重复。
- 安装 Mekanism 后，以上流体仓以及普通输入/输出总成可以同时处理 Mekanism 气体。流体容量以 mB 显示。

1.4.0 修复了四重流体仓读档后每槽容量错误变成 25 mB 的问题。仓室会保存自身等级并在加载时恢复正确的每槽容量。

### 物品、流体和数据总成

`data_input_assembly_hatch`、`input_assembly_hatch`、`output_assembly_hatch` 和 `self_cycle_assembly_hatch` 都有小型、中型、大型、巨型、超级五档（具体物品槽数和流体容量见游戏内提示及配置文件）。

- **数据输入总成**：物品/流体（以及 Mekanism 气体）输入，同时提供 MMCE 智能数据接口；同配方组的数据检查优先采用本仓数值。
- **输入总成**：普通物品、流体和气体输入，不提供数据接口。
- **输出总成**：接收机器产物并向外输出；不同流体槽可以存放相同流体。
- **自循环总成**：优先返还本次配方实际从本仓抽取的同类输入，适合需要循环介质的机器；其他产物不会进入本仓。

容量在 `config/mmce_complement.cfg` 的 `data_input_assembly`、`input_assembly` 中按档位配置，单位为 mB。

### 其他仓室

- **流体释能输入仓**（`liquid_energizer_hatch`）：将配置允许的流体按 `fluid_registry_name=energy_per_mB` 比例直接转换为机器能源。每档流体/能源容量可在 `liquid_energizer_hatch` 配置。
- **过滤物品输出仓**、**过滤流体输出仓**（`filtered_item_output_hatch`、`filtered_fluid_output_hatch`）：单槽过滤输出，容量为 2,147,483,647；匹配的配方产物会优先进入本仓。
- **机械视窗**（`machine_glass`）和多种**机械外壳**（`blockcasing`）用于机器结构和外观。

## ME 网络功能

ME 方块需要连接有效且有供电的 AE2 网络。除特别说明外，ME 设备的标记均可通过 JEI 拖入配置槽。

### ME 能源与魔力总线

| 注册 ID | 名称 | 作用 |
| --- | --- | --- |
| `me_energy_input_bus` / `me_energy_output_bus` | ME机械能源输入/输出总线 | 在机器能源仓与 CrazyAE 能源频道之间传输能源 |
| `me_mana_input_bus` / `me_mana_output_bus` | ME机械魔力输入/输出总线 | 在机器魔力仓与 CrazyAE/Modular Magic 魔力频道之间传输魔力 |

两类总线都可以在 GUI 中调整缓存容量，并显示当前缓存及频道占用；实际传输由对应 AE 存储频道和机器端能力决定。

### ME 库存输入总线

以下四种总线都支持 16 个配置槽，并且采用相同的主动拉取按钮风格：

| 注册 ID | 名称 | 标记类型 |
| --- | --- | --- |
| `me_ore_dict_input_bus` | ME机械矿辞库存输入总线 | 矿物词典表达式 |
| `me_item_inventory_input_bus` | ME机械物品库存输入总线 | 物品 |
| `me_fluid_inventory_input_bus` | ME机械流体库存输入总线 | 流体 |
| `me_gas_inventory_input_bus` | ME机械气体库存输入总线 | 气体（需 Mekanism Energistics） |

行为规则：

1. 标记数量固定显示为 **1**，不可修改；该数字只代表“标记了这种类型”，不代表拉取数量。
2. **主动拉取开启**时，每个标记会从 AE 网络拉取该类型当前可用的全部库存（受槽位容量和“永久保留库存”限制）。
3. **主动拉取关闭**时，标记仍然保留，但设备处于待机状态，不会主动拉取；已经缓存的内容会尝试返回 AE 网络。
4. **永久保留库存**默认为 0。物品单位为数量，流体和气体单位为 mB；抽取后网络剩余量始终不会低于该值。例如网络中有 32 个铁锭、保留值为 16 时最多推送 16 个。
5. 矿辞总线额外提供白名单/黑名单表达式过滤。开启主动拉取后会从匹配的库存中自动选择并维持最多 16 种标记；关闭后可以手动重新设置标记。

### ME 机械总成

- **ME机械输入总成**（`me_input_assembly`）：16 个混合槽位，每个槽位可标记物品、流体或气体并作为同一输入组使用。流体和气体按普通格子大小渲染，不会拉成长条。
- **ME机械库存输入总成**（`me_inventory_input_assembly`）：在混合输入总成基础上增加主动拉取开关和永久保留库存设置。
- **ME机械输出总成**（`me_output_assembly`）：16 个混合输出槽，自动将机器产物推入 AE 网络。
- **ME机械全暴露总成**（`me_full_exposure_assembly`）：无过滤器。开启主动拉取后，从当前 AE 网络拉取所有物品、流体和气体，最多展示 16 个条目；关闭后停止拉取并将缓存回推网络。

四种总成使用 `§b` 动态染色以区别普通总线；本地化名称本身不会额外改变颜色。

### ME 机械样板供应器 II

注册 ID 为 `me_pattern_provider_ii`。它继承 MMCE 样板供应器的网络和内存卡协议，但扩展为：

- 144 个样板槽，布局为 **8 行 × 18 列**；
- 右下角独立存储为 **9 个物品槽 + 3 个流体槽**；流体槽右下角带 `F` 标记；
- GUI、按钮、工作模式、挥手动画和空手潜行右键打开组设置的操作与 MMCE 原版一致；
- 物品数量提示沿用原版实际库存显示，不再显示会误导玩家的额外样板工具提示；
- 支持默认、阻塞、制作锁定、增强阻塞、增强隔离输入等原版工作模式；
- 兼容 MMCE 的**ME机械样板镜像**，以及 Whimcraft 0.1.4 的**ME机械样板供应器库存共享总线**。共享总线共享公共物品/流体处理器，不会把 144 个样板槽或独立输入模式拆成 144 份库存。

### ME 连接共享仓（1.4.1+）

**ME连接共享仓**（`me_connection_share_hatch`）用于让同一台已成型机器内的多个 ME 仓室共用一条外部 AE 网络连接。把共享仓写入机器结构并用 ME 线缆连接到网络后，机器内尚未接线的 ME 输入/输出总线、ME 机械总成、样板供应器和 ME 频道输入仓会自动加入该网络，不必再为每个仓室单独布线。

使用规则：

1. 共享仓必须属于已成型机器，并连接到有效且有电的 AE 网络；它使用致密连接能力，自身不消耗频道。
2. 只共享给**尚未连接外部网络**的本机 ME 仓室。已经接入其他外部 AE 网络的仓室保持原连接，不会被强制并入共享网络。
3. 被共享的普通 ME 仓室仍按原规则消耗频道；ME 频道输入仓按配方实际申请的频道数计费。共享连接只代替线缆，不会提供免费频道。
4. 一台机器可放置多个共享仓。它们连接同一个 AE 网络时可作为重复接入点，并会正确计算重复连接产生的频道占用；若连接到不同网络，配方检查失败，运行中的配方也会暂停并显示“ME连接共享仓连接到了不同的ME网络”。
5. 机器结构重检、共享仓断线、网络重组或仓室拆除后，连接会自动刷新；机器解构时由共享功能建立的临时连接会被清理。

最常见的结构写法是：在机器外壳上预留一个 `mmce_complement:me_connection_share_hatch`，其余 ME 仓室只需作为结构组件存在于同一台机器中。玩家仅给共享仓接线，即可让这些未接线仓室访问同一网络。

## ME 频道输入仓

**ME频道输入仓**（`me_channel_input_hatch`）可以直接连接 AE2 ME 线缆。空闲时不占用频道；只有配方运行期间才会动态申请频道。

- 配方需求必须是大于 0 的整数。
- 机器开始运行前，会检查所有连接到该机器的频道仓能否在同一 AE 网络中提供足够频道；共享线缆只计算一次容量。
- 配方运行期间保留需求数量，配方结束、取消或机器拆除时释放。
- 如果网络重算、断线、控制器冲突，或运行中可用频道降到需求以下，配方会暂停并显示对应错误。
- JEI 已注册专用频道图标和数量显示。

CraftTweaker 配方示例：

```zenscript
// 需要 8 个可用 ME 频道
mods.modularmachinery.RecipeBuilder.newBuilder(
    "channel_recipe", "my_machine", 200
)
    .addMEChannelInput(8)
    // 继续添加普通输入、输出、能源和时间
    .build();
```

## 红石仓室与数据接口

### 红石控制仓

**红石控制仓**（`redstone_control_hatch`）是可被红石主动连接的机器控制器。它读取**自身**收到的红石信号；当信号强度大于等于关闭阈值时，机器暂停，效果等同于控制器被红石充能。

- 普通右键：阈值增加 1，15 后循环回 1，并在快捷栏状态提示。
- Shift + 右键：阈值重置为 1。
- 方块有开启/关闭覆盖层，可随机器外壳颜色变化。

### 红石信号输入/输出仓

这两个方块（`redstone_signal_input_hatch`、`redstone_signal_output_hatch`）是机器与外部红石电路之间的命名数据接口，均可被红石连接。

在 CraftTweaker 中注册机器专属数值：

```zenscript
import mods.mmce_complement.RedstoneInterface;

// 默认 operator=0：取同名输入仓信号的最大值
RedstoneInterface.newRedstone("mymod:my_machine", "temperature").build();
// operator=1：最小值；operator=2：所有同名输入仓信号之和
RedstoneInterface.newRedstone("mymod:my_machine", "pressure", 1).build();
RedstoneInterface.newRedstone("mymod:my_machine", "throughput", 2).build();
```

| `operator` | 聚合方式 |
| --- | --- |
| `0`（默认） | 取所有同名红石输入仓信号的最大值 |
| `1` | 取最小值 |
| `2` | 对所有同名输入仓信号求和 |

在控制器事件脚本中读写数值：

```zenscript
val current = controller.getRedstone("temperature");
controller.setRedstone("throughput", 15); // 自动限制到 0–15
```

输入仓读取送入自身的本地信号，再按注册规则聚合同名输入仓；输出仓在当前机器事件 tick 输出脚本设置的值，且仓室 GUI 中选择的名称必须与脚本名称一致。未注册名称返回 0，重复注册会记录 CraftTweaker 错误。

## 无线通量仓

安装 Flux Networks 后注册：

- `flux_input_hatch`：从无线通量网络接收能源；
- `flux_output_hatch`：向无线通量网络发送能源。

两者都支持网络 ID、名称、优先级、浪涌/禁用限制、区块加载等 Flux Networks 设置。1.4.0 默认值为：

- 内部缓存容量：**10,000 FE**；
- 默认传输速率：**800,000 FE/t**。

## CraftTweaker 与附属模块 API

附属模块的 JSON 格式、依赖/冲突、JEI 预览和结构导出请参阅[附属模块指南](attachment-modules-zh_cn.md)。常用脚本扩展如下：

```zenscript
// 配方要求/禁止附属模块
recipe.withModule(["cooling_compressor"]);
recipe.withoutModule(["low_pressure_pipe"]);

// 查询控制器上的附属模块
if (controller.hasModule("cooling_compressor")) {
    // 模块已生效
}
val modules = controller.moduleList;
```

`addMEChannelInput(int)`、`RedstoneInterface.newRedstone(...).build()`、`controller.getRedstone(...)` 和 `controller.setRedstone(...)` 只在相应集成可用时才有实际效果。

### 配方输出修改器

MMCE Complement 扩展了 `mods.modularmachinery.RecipePrimer`，可在对应输出后立即添加 CraftTweaker 修改器：

```zenscript
mods.modularmachinery.RecipeBuilder.newBuilder("dynamic", "my_machine", 200)
    .addFluidOutput(<liquid:water> * 1000)
    .addFluidModifier(function(controller, liquid) {
        return <liquid:lava> * (liquid.amount * 2);
    })
    .addGasOutput(<gas:hydrogen> * 1000)
    .addGasModifier(function(controller, gas) {
        return <gas:oxygen> * (gas.amount * 2);
    })
    .build();
```

- `.addFluidModifier(function(controller, liquid) as ILiquidStack)` 修改流体输出的种类、数量和 NBT；返回 `null` 或非正数量会取消该输出。
- `.addGasModifier(function(controller, gas))` 修改气体输出的种类和数量；气体参数/返回值在 ZenScript 中表现为 `IIngredient`，运行时必须是 Mekanism `IGasStack`。
- 修改器必须紧跟对应的流体或气体输出；多个修改器按声明顺序执行，并参与输出空间预检、并行数量计算和最终写入。
- 详细说明：[流体输出修改器](fluid-output-modifier-zh_cn.md)、[气体输出修改器](gas-output-modifier-zh_cn.md)。

### 流体预览 NBT 与气体 Tooltip

`.setPreViewNBT(IData)` 可用于紧随物品或流体输入/输出之后，为 JEI 预览设置显示 NBT：

```zenscript
recipe
    .addFluidOutput(<liquid:water> * 1000)
    .setPreViewNBT({Potion: "healing"});

recipe
    .addGasOutput(<gas:hydrogen> * 1000)
    .setGasTooltip("纯度：高", "经过压缩处理");
```

流体 NBT 会显示在 JEI 流体槽中，不改变实际输入匹配或输出内容。气体不支持 `.setPreViewNBT(IData)`；Mekanism `GasStack` 不支持任意 NBT，气体仅提供 Tooltip API。`.addGasTooltip(String...)` 追加提示行，`.setGasTooltip(String...)` 覆盖此前由本扩展添加的提示行；两者均支持气体输入和输出组件。

详细说明：[流体/气体预览 NBT](preview-nbt-zh_cn.md)。

附属模块结构检查与 MMCE 主结构使用相同的同步检查流程、调度和线程，不再存在独立的逐 tick 附属区域扫描或变化监听轮询。

## 1.4.x 更新摘要

- 新增 ME 物品/流体/气体/矿辞库存输入总线的统一标记、主动拉取和永久保留库存功能。
- 新增 ME 输入、库存输入、输出、全暴露四种混合总成。
- 新增 ME 频道输入仓及 JEI 支持，提供动态频道占用和 CraftTweaker `.addMEChannelInput(int)`。
- 新增红石控制仓、红石信号输入仓和红石信号输出仓，以及命名红石接口 API。
- 新增 144 槽的 ME 机械样板供应器 II，并兼容 MMCE 样板镜像和 Whimcraft 库存共享总线。
- 新增 ME 连接共享仓，一条外部 AE 网络连接即可供同一机器内未接线的 ME 仓室共同使用，并保留正常频道计费和异网冲突保护。
- 输入总成可在同一配方组中同时作为物品输入仓和流体/气体输入仓使用；数据输入总成还可同时承担智能数据接口需求。
- 无线通量仓默认缓存/速率更新为 10,000 FE / 800,000 FE/t。
- 修复四重流体仓读档容量、样板供应器 II GUI/材质/交互、ME 总成贴图和红石控制仓连接等问题。
- 修复 Nova Engineering 的可选星辉魔法（Astral Sorcery）兼容层在配方注册表尚未编译时被提前查询而导致的启动异常。

## 常见问题

**找不到 ME 或气体方块？** 检查 AE2 Extended Life、CrazyAE、Mekanism 和 Mekanism Energistics 是否安装完整；模组会按依赖动态注册方块。

**ME 频道配方无法启动？** 确认所有 ME 频道输入仓接入同一个有电 AE 网络，等待网络完成重算，并检查可分配频道数是否足够。

**库存输入总线没有拉取？** 确认“主动拉取”为开启；关闭时是待机状态。永久保留库存值过高也会使可抽取数量变为 0。

**流体仓容量不正确？** 破坏并重新放置旧版本方块，或让区块重新保存；1.4.0 会保存并恢复方块等级，不再把每槽容量重置为 25 mB。
