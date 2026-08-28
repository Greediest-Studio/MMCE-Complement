# MMCE Complement 附属模块指南

[中文 Wiki 总览](wiki-zh_cn.md) · [English documentation](attachment-modules-en_us.md)

本文面向整合包制作者，介绍如何为已有的 Modular Machinery Community Edition（MMCE）机器添加可选多方块结构、模块依赖、模块互斥、JEI 预览和配方限制。

## 一分钟了解附属模块

- 原 MMCE 机器 JSON 顶层的 `parts` 仍然定义主结构，主结构的保留 ID 为 `main`。
- 新增的顶层 `modules` 数组定义零个或多个附属模块；每个模块都有唯一 ID 和自己的原生 MMCE `parts`。
- 控制器先识别并形成 `main`，再检查各附属模块的方块、依赖和冲突关系。
- 当前生效模块的结构会加入机器，其仓室、总线、选择标签等模块化方块可正常参与机器工作和外观变色。
- 附属模块默认同时提供一个同 ID 的 MMCE 升级，可用 `"as-upgrade": false` 关闭。
- CraftTweaker 配方可要求或禁止指定模块，脚本也可查询控制器的模块状态。

“已声明”和“已生效”不是一回事：`modules` 中出现的 ID 是已声明模块；只有结构匹配、全部依赖满足且没有冲突时，该模块才会生效。

## 快速上手

假设已有机器 `distillation_tower.json`：

1. 保留原来的 `parts`，它就是 `main`。
2. 在机器 JSON 顶层添加 `modules` 数组。
3. 为每个模块填写唯一的 `id` 和 `parts`。
4. 不填写 `depends-on` 时，模块默认依赖 `main`；需要模块链时显式填写父模块 ID。
5. 启动或重载机器定义，先搭好主结构，再搭附属结构。
6. 在 JEI 结构预览中切换页面，检查坐标和父子合并效果。
7. 如需限制配方，在 RecipeBuilder 链中加入 `.withModule(...)` 或 `.withoutModule(...)`。

最小配置如下：

```json
{
  "registryname": "distillation_tower",
  "localizedname": "蒸馏塔",
  "parts": [
    { "x": 0, "y": -1, "z": 0, "elements": ["modularmachinery:blockcasing"] }
  ],
  "modules": [
    {
      "id": "cooling_compressor",
      "parts": [
        { "x": 3, "y": 0, "z": 0, "elements": ["minecraft:iron_block"] }
      ]
    }
  ]
}
```

不要在 `modules` 中再写一个 `main` 对象，也不要把控制器方块写入模块 `parts`。控制器仍由 MMCE 的主机器定义负责。

## 完整 JSON 示例

下面的例子展示默认依赖、依赖链、平行模块、冲突关系以及关闭升级功能。所有被引用的模块都已声明，因此可以直接作为模板使用。

```json
{
  "registryname": "distillation_tower",
  "localizedname": "蒸馏塔",
  "parts": [
    { "x": 0, "y": -1, "z": 0, "elements": ["modularmachinery:blockcasing"] },
    { "x": 0, "y": -2, "z": 0, "elements": ["minecraft:iron_block"] }
  ],
  "modules": [
    {
      "id": "cooling_compressor",
      "parts": [
        { "x": 3, "y": 0, "z": 0, "elements": ["minecraft:iron_block"] },
        { "x": 3, "y": 1, "z": 0, "elements": ["minecraft:packed_ice"] }
      ]
    },
    {
      "id": "spacetime_pipe",
      "depends-on": ["cooling_compressor"],
      "conflicts-with": ["low_pressure_pipe"],
      "parts": [
        { "x": 4, "y": 0, "z": 0, "elements": ["minecraft:diamond_block"] }
      ]
    },
    {
      "id": "low_pressure_pipe",
      "conflicts-with": ["spacetime_pipe"],
      "parts": [
        { "x": -3, "y": 0, "z": 0, "elements": ["minecraft:gold_block"] }
      ]
    },
    {
      "id": "alternate_shell",
      "depends-on": [],
      "as-upgrade": false,
      "parts": [
        { "x": 0, "y": -3, "z": 0, "elements": ["minecraft:glass"] }
      ]
    }
  ]
}
```

## JSON 字段参考

`modules` 必须位于机器 JSON 顶层，并且必须是数组。没有附属模块的旧机器无需添加该字段。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | 字符串 | 是 | 无 | 当前机器内唯一的模块 ID，区分大小写；首尾空白会被移除。`main` 是保留值。 |
| `parts` | 数组 | 是 | 无 | 与 MMCE 主结构 `parts` 相同的格式和坐标系，支持 MMCE 原有的方块候选、NBT 和组件选择标签。 |
| `depends-on` | 字符串数组 | 否 | `["main"]` | 模块生效所需的全部父模块。写 `[]` 表示没有图关系上的父模块。 |
| `conflicts-with` | 字符串数组 | 否 | `[]` | 与本模块互斥的模块 ID。运行时按双向冲突处理。 |
| `as-upgrade` | 布尔值 | 否 | `true` | 生效时是否提供一个与 `id` 同名的 MMCE 升级。 |

注意：

- `parts` 必须是数组，即使使用的是 MMCE 支持的复杂方块定义，也应保持原生 `parts` 条目格式。
- `depends-on` 和 `conflicts-with` 中的每个 ID 必须是 `main` 或同一机器中已声明的模块。
- 重复 ID、未知引用、空 ID、自依赖、自冲突和循环依赖都会在机器 JSON 加载阶段报错。
- 同一个数组中的重复关系会自动去重，但不建议依赖这一行为。
- 当前只应在模块对象中使用上表字段；模块不会继承一份完整机器 JSON 的其他顶层设置。

## 坐标、方向和结构重叠

模块 `parts` 与主结构使用完全相同的坐标规则：控制器为 `(0, 0, 0)`，坐标会随控制器朝向一起旋转。因此，最稳妥的做法是让模块坐标直接延续主机器 JSON 的坐标系。

父子结构重叠时遵循“父模块拥有坐标”的规则：

- 子模块和任意祖先在同一坐标定义不同方块时，子模块在该坐标的定义会被忽略；结构匹配、组件收集和 JEI 预览均以父模块为准。
- 父子模块在同一坐标定义完全相同的方块信息时，可以共享该方块。
- 规则会沿 `depends-on` 递归到所有祖先，包括通常作为根的 `main`。

不建议让互相没有依赖关系的平行/兄弟模块占用同一坐标，因为它们没有明确的父子所有权。请使用不同坐标，或通过 `depends-on` 明确所有权。

`"depends-on": []` 只表示模块在依赖图和 JEI 合并预览中与 `main` 平行。控制器仍必须先识别并形成主机器，附属模块才会被检查。

## 生效、依赖和冲突规则

每次 MMCE 对 `main` 执行结构检查并确认主结构仍然成型时，控制器会在同一次检查流程中按以下顺序决定当前生效模块：

1. 检查每个模块的有效 `parts` 是否在世界中匹配。
2. 仅保留所有直接及间接依赖均已匹配并生效的模块。
3. 如果两个结构已匹配且依赖有效的模块互相冲突，则冲突双方都不生效。
4. 依赖冲突模块的子模块也会因缺少父模块而失效。

例如 `c` 依赖 `b`、`b` 依赖 `a`：只有 `a`、`b`、`c` 的结构全部存在时，`c` 才会生效。只搭 `a` 和 `c` 不会让 `c` 生效。

冲突只需在一侧声明即可产生双向效果，但为了配置可读性，建议双方都写明。若 `a` 与 `b` 冲突且两者本来都能生效，则 `a` 和 `b` 都会关闭，而不是按 JSON 顺序选择其中一个。

模块状态改变后，控制器会重新搜索配方，并更新当前结构组件。生效模块中的输入/输出仓、总线和其他 MMCE 模块化方块会加入机器；模块失效后会被移除。

异步配方搜索线程不会计算附属模块条件。配方选中后只在开始前的 pre-tick 边界检查一次，并在结束 tick 再检查一次；中间的普通运行 tick 不再重复检查附属模块结构。

附属模块匹配现在与主结构使用完全相同的同步结构检查流程、调度和线程。不存在独立的逐 tick 附属区域扫描或变化监听轮询。若附属区域暂时未加载，控制器会保留上一次有效状态，待下一次同步结构检查能够访问该区域时再检查。

## 模块作为 MMCE 升级

`as-upgrade` 默认为 `true`。模块生效时，控制器会提供一个名称等于模块 ID 的合成 MMCE 升级，因此已有的升级查询或配方逻辑可以复用模块 ID。

```json
{
  "id": "decorative_shell",
  "as-upgrade": false,
  "parts": [
    { "x": 2, "y": 0, "z": 0, "elements": ["minecraft:glass"] }
  ]
}
```

设为 `false` 只关闭“作为升级”这一效果。该模块仍会正常匹配、加入结构，也仍可成为其他模块的依赖。

## 使用附属模块区域选择工具

“附属模块区域选择工具”位于 MMCE 创造物品栏。使用者必须处于创造模式并拥有管理员命令权限。

它继承 MMCE 原版 `ItemConstructTool`，所以：

- 右键普通方块的选择/取消选择方式与原版相同。
- 白色边框高亮与原版相同。
- 选区直接使用 MMCE 的玩家选区，可与原版及其他兼容 `ItemConstructTool` 的工具交替使用。

### 从主机器导出第一个模块

1. 让主机器完整成型并被控制器识别。
2. 在世界中搭建新模块。
3. 用工具选中所有新模块方块。选区包含一部分主结构也没关系。
4. 手持本工具，潜行右键已经成型的控制器。
5. 工具会从选区扣除控制器当前成型结构中的全部坐标，只导出剩余方块。
6. 将剪贴板中的单个模块对象粘贴到机器 JSON 的 `modules` 数组中，再修改自动生成的 ID。

### 在已有模块上继续导出

先让已有模块生效，再按相同方式选择并导出。扣除范围包括 `main`、全部当前生效的附属模块以及控制器当前成型模式中的其他方块。因此，在模块 `1` 上制作模块 `2` 时，导出结果不会重复包含 `main` 或模块 `1`。

导出器会把当时所有生效模块 ID 写入新对象的 `depends-on`。这代表“全部都必须生效”；若实际只需要其中一个直接父模块，请在粘贴后手动精简该数组。

如果当前没有生效的附属模块，导出对象会省略 `depends-on`，加载时等价于默认的 `["main"]`。

### 导出结果与文件

输出是一个模块对象，不是完整机器 JSON，例如：

```json
{
  "id": "attachment_20260724_190000",
  "depends-on": [
    "cooling_compressor"
  ],
  "parts": [
    { "x": 4, "y": 0, "z": 0, "elements": ["minecraft:diamond_block"] }
  ]
}
```

内容会复制到客户端剪贴板，并以 `attachment-module-玩家名-模块ID.json` 保存到 MMCE 当前的 machinery 目录。粘贴进正式机器 JSON 后，请将这个独立导出文件移出 machinery 目录或删除；它只是模块对象，不能被当成完整机器 JSON 单独加载。

若潜行右键的控制器尚未成型，工具会回退到 MMCE 原版流程并导出完整结构 JSON。无论成功或失败，本次导出后选区都会被清空。

## JEI 结构预览

拥有 `modules` 的机器会在原 MMCE 结构预览右下角增加按钮：

- 模块切换按钮：按 `main`、随后按 JSON 声明顺序循环各模块页面；tooltip 显示当前模块。
- 与父模块合并展示按钮：仅当当前附属模块拥有父模块时显示在切换按钮左侧，默认关闭。

`main` 页面展示原主结构。附属模块独立页面只展示该模块的有效方块，并保留主控制器作为定位参考。开启父模块合并后，会同时展示当前模块及其全部直接/间接父模块，一直追溯到依赖链根部（通常为 `main`）。

JEI 页面展示的是模块定义和父子覆盖规则，不代表世界中该模块此刻已经生效。

## CraftTweaker 配方限制

本模组扩展了 `mods.modularmachinery.RecipePrimer`：

```zenscript
withModule(string[] ids) as RecipePrimer
withoutModule(string[] ids) as RecipePrimer
```

- `withModule`：数组中的模块必须全部处于生效状态。
- `withoutModule`：数组中只要有任意模块生效，配方就不能运行。
- 可以重复调用；限制会累加，重复 ID 会去重。
- 模块 ID 不能为空，也不能是保留 ID `main`。
- 同一配方不能同时要求和禁止同一个模块，否则脚本创建配方时会报错。

```zenscript
mods.modularmachinery.RecipeBuilder.newBuilder(
    "distillation_with_cooling",
    "distillation_tower",
    200
)
    .withModule(["cooling_compressor", "spacetime_pipe"])
    .withoutModule(["low_pressure_pipe"])
    .build();
```

模块状态只在机器结构检查任务中解析。带模块限制的配方在开始前的 pre-tick 边界读取一次结果，并在结束 tick 再读取一次；运行中的普通 tick 不会复制活动模块集合或执行模块成型判断。运行途中失去必需模块或出现禁止模块，会在结构检查刷新快照后的下一个边界返回相应失败状态。

## CraftTweaker 控制器查询

本模组还扩展了 `mods.modularmachinery.IMachineController`：

```zenscript
// 指定附属模块当前是否生效。
val coolingActive as bool = controller.hasModule("cooling_compressor");

// 当前已识别机器声明的所有附属模块 ID；保持 JSON 顺序，不含 main。
val declaredModules as string[] = controller.moduleList;
```

| 成员 | 返回内容 |
| --- | --- |
| `hasModule(string)` | 当前控制器上指定模块是否生效。未生效、未声明和 `main` 都返回 `false`。 |
| `moduleList` | 当前已识别机器声明的全部附属模块 ID，不是当前生效模块列表；控制器尚未识别机器时返回空数组。 |

若需要当前生效模块列表，可遍历 `moduleList` 并逐个调用 `hasModule`。

## 推荐的模块设计方式

- 线性升级：`main <- cooling <- compression <- spacetime`，每一级只依赖它的直接父模块。
- 多前置升级：某模块使用 `"depends-on": ["cooling", "power_booster"]`，表示两个模块缺一不可。
- 二选一外观：两个模块都依赖 `main` 并互相写入 `conflicts-with`。
- 平行外观：使用 `"depends-on": []`；如果不应影响升级逻辑，再设置 `"as-upgrade": false`。
- 避免把大量当前模块全部写成父模块。依赖越精确，JEI 合并预览和后续模块扩展越容易理解。

## 常见问题排查

### 机器 JSON 无法加载

检查日志中的 attachment module 报错，并确认：

- `modules` 是顶层数组，每个元素都是对象。
- 每个对象都有字符串 `id` 和数组 `parts`。
- 没有使用 `main`、重复 ID、空 ID、自依赖、自冲突或依赖环。
- `depends-on` 和 `conflicts-with` 引用的模块都已在同一机器中声明。
- 独立的工具导出文件已经移出 machinery 目录。

### 模块方块已经搭好，但模块不生效

- 先确认主机器已经成型且朝向正确。
- 在 JEI 单模块页核对相对坐标；坐标会随控制器旋转。
- 确认所有父模块均已搭建并生效，而不只是结构存在。
- 检查是否有已匹配的冲突模块；冲突会让双方同时失效。
- 检查与父结构重叠的位置。若定义不同，该位置由父模块负责，子模块定义不会参与匹配。

### 选择工具没有导出附属方块

- 确认使用的是“附属模块区域选择工具”，玩家处于创造模式且有管理员权限。
- 确认使用潜行右键控制器，并且选区中至少有一个不属于当前成型结构的新方块。
- 选区只包含 `main` 和当前生效模块时，扣除后为空，工具会提示无法导出。
- 若希望自动排除某个已有模块，必须先让它真正生效。

### 配方条件与预期不一致

- `withModule(["a", "b"])` 是 AND：`a` 和 `b` 都必须生效。
- `withoutModule(["a", "b"])` 是禁止集合：`a` 或 `b` 任意一个生效都会阻止配方。
- `moduleList` 是声明列表；判断运行状态必须使用 `hasModule`。
- 模块 ID 区分大小写，并应与机器 JSON 完全一致。
