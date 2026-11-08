# CraftTweaker 支持

脚本放在实例的 `scripts` 目录中。本章列出 Complement 对 MMCE 配方构建器和控制器的扩展；物品写法沿用 Minecraft 1.12.2 的 CraftTweaker 2 语法。

| 接口 | 功能 |
| --- | --- |
| `RecipePrimer` | 流体/气体催化剂、模块条件、ME 频道输入、输出修改器和预览信息 |
| `IMachineController` | 附属模块查询、命名红石读写、存储控制器转换 |
| `IMachineStorageController` | 物品/流体/气体查询和存取、动态槽数、单槽容量、唯一槽位编号 |
| `RedstoneInterface` | 注册机器专属红石数据及聚合方式 |

## 催化剂输入

```zenscript
.addFluidCatalystInput(ILiquidStack stack, string[] tooltips, RecipeModifier[] modifiers) as RecipePrimer
.addGasCatalystInput(IGasStack stack, string[] tooltips, RecipeModifier[] modifiers) as RecipePrimer
.setMaxCatalyst(int max) as RecipePrimer
```

流体和气体催化剂与 MMCE 原版物品催化剂拥有相同的可选检查、chance 消耗和 modifier 语义。`setMaxCatalyst` 只限制一次运行最多实际激活的催化剂种类；配方仍可声明更多催化剂。未选中的催化剂不会抽取或应用 modifier。气体方法需要 Mekanism。

本模组扩展 MMCE 的 `RecipePrimer` 与 `IMachineController`。

## 存储控制器

脚本类型为 `mods.modularmachinery.IMachineStorageController`，继承 MMCE 的 `IMachineController`。适用于本模组的多方块存储控制器；普通配方机器不能转换为此类型。AE2UEL 与 AE2S 使用同一套 API。

### 获取接口

对现有 `IMachineController` 调用 `asStorageController()`，返回存储控制器或 `null`：

```zenscript
import mods.modularmachinery.IMachineController;
import mods.modularmachinery.IMachineStorageController;

function inspectStorage(controller as IMachineController) {
    val storage as IMachineStorageController = controller.asStorageController();
    if (storage == null) return;

    print("物品槽数：" ~ storage.maxItemSlots);
    print("流体槽数：" ~ storage.maxFluidSlots);
    print("气体槽数：" ~ storage.maxGasSlots);
    print("每个流体槽容量（mB）：" ~ storage.fluidSlotCapacity);
    print("每个气体槽容量（Mekanism 单位）：" ~ storage.gasSlotCapacity);

    if (!storage.storageAvailable) return;
    val firstItem = storage.getItemInSlot(0);
    if (firstItem != null) print(firstItem);
}
```

可以在现有机器事件中将 `event.controller` 传入上述函数，也可以将 MMCE 的 `MachineController.getControllerAt(...)` 返回值传入。存储器成型后会在每个服务端 tick 触发原有的 `MachineTickEvent` START、END 两个阶段，自动输入/输出发生在二者之间；事件不包含配方执行。

### 配置和可用性

| 属性 / 方法 | 类型 | 说明 |
| --- | --- | --- |
| `maxItemSlots` | int | 配置允许的最大物品槽数 |
| `maxFluidSlots` | int | 配置允许的最大流体槽数 |
| `maxGasSlots` | int | 配置允许的最大气体槽数 |
| `fluidSlotCapacity` | int | 每个流体槽的容量，mB |
| `gasSlotCapacity` | int | 每个气体槽的容量，Mekanism 单位 |
| `getFluidCapacityInSlot(index)` | int | 指定流体槽的容量 |
| `getGasCapacityInSlot(index)` | int | 指定气体槽的容量 |
| `storageAvailable` | bool | 当前结构已成型，且调用发生在服务端主线程 |

未开启的类型返回 0；未安装 Mekanism 时气体槽数和容量也为 0。容量查询返回配置上限，不是剩余容量或非空槽数，结构未成型时仍可查询。槽位越界时容量为 0。

`fluidSlotCapacity` / `gasSlotCapacity` 表示该类型的默认单槽容量；设置过个别槽位后，
实际容量应逐槽调用 `getFluidCapacityInSlot(index)` / `getGasCapacityInSlot(index)`。

库存查询与存取仅在 `storageAvailable` 为 true 时工作；未成型、失效、客户端、异步线程或未连接服务端世界时，返回 `null` / 0 / false，不初始化或暴露被封存的库存。应在服务端主线程机器事件中操作库存。

### 动态修改槽数和容量

以下方法的 `count`、`index`、`capacity` 参数均为 `int`，返回值为 `bool`。仅在 `storageAvailable` 为 true 时执行。修改只影响当前这台机器，
不会修改共享机器 JSON 或其他控制器，随控制器 NBT 保存并同步，卸载区块、重启和临时拆坏结构后仍保留。

| 方法 | 作用 |
| --- | --- |
| `setMaxItemSlots(count)` | 修改物品槽总数 |
| `setMaxFluidSlots(count)` | 修改已开启的流体槽总数 |
| `setMaxGasSlots(count)` | 修改已开启的气体槽总数，需要 Mekanism |
| `setFluidCapacityInSlot(index, capacity)` | 修改指定流体槽容量，单位 mB |
| `setGasCapacityInSlot(index, capacity)` | 修改指定气体槽容量，Mekanism 原始单位 |
| `setFluidSlotCapacity(capacity)` | 设置所有现有流体槽及未来新增槽的默认容量，清除单槽覆盖 |
| `setGasSlotCapacity(capacity)` | 设置所有现有气体槽及未来新增槽的默认容量，清除单槽覆盖 |

- 槽数范围为 **1–4096**，容量范围为 **1–2,147,483,647**。不能用这些方法开启 JSON 中未配置的流体/气体类型。
- 缩减物品槽数时，按索引顺序将末尾槽位的物品搬入保留区的空槽，遵守物品堆叠上限，保留 metadata 和 NBT。现有非空槽保持原样；放不下的物品从控制器位置掉落，行为与破坏控制器时一致。
- 缩减流体或气体槽数时，被删除的末尾槽位必须为空。缩减单槽容量时，新容量不能低于已有数量。
- 若物品实体生成被其他模组取消，未掉落的余量保留在原末尾槽，槽数设置返回 `false`；已经搬移或掉落的物品不会再次生成。
- 缩减单槽容量时，新值不得小于该槽现有数量。批量设置会先检查全部槽位，任意一个不满足即整次拒绝。
- 无效参数、越界、类型未开启、结构未成型、客户端或异步调用均返回 `false`，原配置和内容不变。
- 成功或重复设置相同值返回 `true`；重复调用相同设置不会持续产生脏数据通知。
- 扩容保留现有槽位和编号；新增槽位使用该类型默认容量。单槽容量固定在槽位上，排序不会将容量随内容搬走。
- 仍使用原来的共享库存处理器，管道和 AE 端口可继续访问；终端数量只在实际内容变化时增减，改变容量不会生成内容。

```zenscript
import mods.modularmachinery.IMachineController;

// 从服务端机器事件按升级条件调用；容量设置不创建物品/流体/气体。
function upgradeStorage(controller as IMachineController) {
    val storage = controller.asStorageController();
    if (storage == null || !storage.storageAvailable) return;

    val itemsResized = storage.setMaxItemSlots(108);
    if (storage.maxFluidSlots > 0) {
        val fluidsResized = storage.setMaxFluidSlots(18);
        val fluidsDefault = storage.setFluidSlotCapacity(16000);
        val largeFluidSlot = storage.setFluidCapacityInSlot(0, 64000);
    }
    if (storage.maxGasSlots > 0) {
        val gasesResized = storage.setMaxGasSlots(18);
        val gasesDefault = storage.setGasSlotCapacity(16000);
        val largeGasSlot = storage.setGasCapacityInSlot(0, 64000);
    }
}
```

多个设置调用各自独立，并非一组事务。降级脚本应检查每个返回值；物品缩槽会搬移并掉落溢出物，流体和气体缩槽前应先清空末尾槽，降低容量前应先取走超量内容。

### 查询和存取

三类内容的参数类型分别为 `IItemStack`、`ILiquidStack`、`IIngredient`；气体必须使用 Mekanism 的真实气体栈，例如 `<gas:hydrogen>`，不支持矿辞、通配符或组合材料。

| 物品方法 | 流体方法 | 气体方法 | 返回值与作用 |
| --- | --- | --- | --- |
| `hasItem(stack)` | `hasFluid(stack)` | `hasGas(stack)` | bool：存在至少一单位匹配内容，忽略请求数量 |
| `countItem(stack)` | `countFluid(stack)` | `countGas(stack)` | long：匹配内容的总量 |
| `insertItem(stack, simulate)` | `insertFluid(stack, simulate)` | `insertGas(stack, simulate)` | int：可添加或实际添加的数量 |
| `extractItem(stack, simulate)` | `extractFluid(stack, simulate)` | `extractGas(stack, simulate)` | int：可提取或实际提取的数量 |
| `insertItemInSlot(index, stack, simulate)` | `insertFluidInSlot(index, stack, simulate)` | `insertGasInSlot(index, stack, simulate)` | int：仅向指定槽添加的数量 |
| `extractItemInSlot(index, stack, simulate)` | `extractFluidInSlot(index, stack, simulate)` | `extractGasInSlot(index, stack, simulate)` | int：仅从指定槽提取的数量 |
| `getItemInSlot(index)` | `getFluidInSlot(index)` | `getGasInSlot(index)` | 对应类型的栈或 null：槽位内容快照 |

`index` 为 int；`simulate` 为可选 bool，默认 `false`。模拟返回当前可操作的数量，不改变库存。物品、流体、气体的参数与返回栈类型分别为 `IItemStack`、`ILiquidStack` 和 `IIngredient`。

物品匹配包含物品、metadata、完整 NBT；流体包含流体种类和 NBT；气体按 Mekanism 气体种类匹配。查询返回独立副本，修改副本不会改变机器。

请求数量取自栈的数量，例如 `<minecraft:diamond> * 10`、`<liquid:water> * 1000`、`<gas:hydrogen> * 1000`。容量不足或库存不足时允许部分执行，以返回数量为准。空参数、非正数量和无效索引不会改变库存。

```zenscript
import mods.modularmachinery.IMachineStorageController;

// 仅在你的服务端事件明确需要执行时调用，不要无条件每 tick 添加物品。
function storageExample(storage as IMachineStorageController) {
    if (!storage.storageAvailable) return;

    val diamonds = storage.countItem(<minecraft:diamond>);
    val waterPresent = storage.hasFluid(<liquid:water>);

    // 预览添加 10 个钻石；不会修改机器。
    val canInsert = storage.insertItem(<minecraft:diamond> * 10, true);

    // 添加到机器，返回实际接受数量；未接受的部分由调用脚本处理。
    val inserted = storage.insertItem(<minecraft:diamond> * 10);

    // 从整个机器提取最多 4 个钻石，返回实际移除数量。
    val extracted = storage.extractItem(<minecraft:diamond> * 4);

    // 仅对流体分类的第 2 个槽（索引 1）进行操作。
    val filled = storage.insertFluidInSlot(1, <liquid:water> * 1000);
    val drained = storage.extractFluidInSlot(1, <liquid:water> * 250);
}
```

`insertX` 直接向机器创建指定内容，`extractX` 直接移除内容；它们不会自动扣除或放入玩家、管道等外部库存。脚本连接外部库存时应按实际返回数量处理外部库存。模拟只预览当前状态，不预留空间或内容。

每次操作直接修改控制器的共享后端，沿用保存、同步和 AE 存储变更通知。多个仓室和脚本访问同一份库存，操作期间使用对应后端的锁，不保存第二份库存；多个独立调用不组成跨库存事务。

### 槽位索引和唯一编号

分类索引从 0 开始，范围为 `0 <= index < maxXSlots`，包括空槽。物品槽索引 0 与流体槽索引 0 是不同槽。

需要跨类型保存槽位身份时，使用统一的 long 编号：

| 方法 | 说明 |
| --- | --- |
| `getSlotId("item"/"fluid"/"gas", index)` | 获取统一编号，非法类型或索引返回 -1 |
| `getSlotType(id)` | 返回 item、fluid、gas；无效编号返回 null |
| `getSlotIndex(id)` | 还原分类索引，无效编号返回 -1 |
| `getItemBySlotId(id)` | 通过统一编号读取物品，类型错误或空槽返回 null |
| `getFluidBySlotId(id)` | 通过统一编号读取流体，类型错误或空槽返回 null |
| `getGasBySlotId(id)` | 通过统一编号读取气体，类型错误或空槽返回 null |

编码为 `index * 3 + type`，type 为物品 0、流体 1、气体 2。例如物品 0、1 为编号 0、3，流体 0、1 为编号 1、4，气体 0、1 为编号 2、5。编号在同一机器内唯一；不包含机器坐标。清空槽位、插入内容、改变其它类型的槽数都不会改变已有槽位编号。缩减配置后被移除的槽位编号失效；排序可能移动内容，但编号仍指向原槽位，不跟随某一份物品。

### 继承接口的行为

位置、世界、朝向、自定义数据、机器模式、结构查询等原接口照常可用。存储控制器不执行配方，因此：

- `activeRecipe` 返回 null。
- `activeRecipeList`、`recipeThreadList` 返回空数组。
- `isWorking` 返回 false；判断存储可用性使用 `storageAvailable`。
- `extraThreadCount` 返回 0，写入忽略。
- `addModifier`、`removeModifier`、`addPermanentModifier`、`removePermanentModifier` 无操作；`hasModifier` 返回 false。

## ME 频道输入

```zenscript
.addMEChannelInput(int amount) as RecipePrimer
```

`amount` 必须是大于 0 的整数。配方开始前会检查同一机器的全部频道仓是否位于同一 AE 网络且可提供足够频道；运行期间保持占用，不足时暂停。完整规则见 [ME 频道输入仓](https://github.com/Greediest-Studio/MMCE-Complement/wiki/ME-Channel)。

```zenscript
mods.modularmachinery.RecipeBuilder.newBuilder(
    "channel_recipe", "my_machine", 200
)
    .addMEChannelInput(8)
    .build();
```

## 配方模块条件

```zenscript
.withModule(string[] ids) as RecipePrimer
.withoutModule(string[] ids) as RecipePrimer
```

- `withModule`：数组内模块必须**全部**生效（AND）。
- `withoutModule`：数组内任意模块生效都会禁止配方。
- 两个方法都返回原 `RecipePrimer`，可继续链式调用。
- 可重复调用，条件会累加并去重。
- ID 不能空白，不能使用保留 ID `main`。
- 同一个 ID 不能同时出现在需要与禁止集合中。

示例：

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

模块结构在主结构检查中解析。配方开始前的 pre-tick 与结束 tick 边界读取模块快照；普通运行 tick 不重复匹配结构。运行中失去必需模块或出现禁止模块，会在检查刷新后的边界返回对应失败状态。

## 控制器查询

```zenscript
controller.hasModule(string id) as bool
controller.moduleList as string[]
```

### `hasModule`

判断模块**当前是否生效**。未声明、结构不完整、依赖不满足、冲突失效以及 `main` 都返回 false。

```zenscript
val upgraded as bool = controller.hasModule("advanced");
```

### `moduleList`

返回当前已识别机器 JSON 中声明的全部附属模块 ID，保持 JSON 顺序，不含 `main`。注意它是“可用/已声明列表”，不是“当前生效列表”。控制器尚未识别机器时返回空数组。

需要所有当前生效模块时，遍历 `moduleList` 并逐个调用 `hasModule`。

## 控制器信息示例

下面的函数可根据模块状态生成控制器说明文字。在已有 `onControllerGUIRender` 回调中，使用 `event.extraInfo = moduleInfo(event.controller);` 显示这些内容。

```zenscript
import mods.modularmachinery.IMachineController;

function moduleInfo(controller as IMachineController) as string[] {
    return [
        "§e大型反应单元",
        "§a附属模块：" +
            (controller.hasModule("advanced") ? "§e升级模块" : "§c无")
    ];
}
```

模块 ID 与 JSON 大小写须完全一致。判断模块状态前，应让主结构完成检测，并满足父模块与冲突条件。可使用适用于 1.12.2 的 NBT 查看工具检查控制器的 `mmceComplementModules` 列表。

## 命名红石数据

注册机器专属数据名：

```zenscript
import mods.mmce_complement.RedstoneInterface;

RedstoneInterface.newRedstone(
    "mymod:my_machine", "temperature"
).build(); // operator=0，最大值

RedstoneInterface.newRedstone(
    "mymod:my_machine", "pressure", 1
).build(); // 最小值

RedstoneInterface.newRedstone(
    "mymod:my_machine", "throughput", 2
).build(); // 求和
```

在控制器事件 tick 中使用：

```zenscript
val input = controller.getRedstone("temperature");
controller.setRedstone("throughput", 15);
```

`setRedstone` 只接受当前机器事件 tick 的输出，数值自动限制在 0～15；仓室标签必须与脚本名称一致。完整规则见 [红石控制与信号接口](https://github.com/Greediest-Studio/MMCE-Complement/wiki/Redstone-Hatches)。

## 流体与气体输出修改器

MMCE Complement 为 `RecipePrimer` 提供以下输出修改器。修改器必须紧跟对应的流体或气体输出，多个修改器按声明顺序执行：

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

- `.addFluidModifier(function(controller, liquid) as ILiquidStack)` 可修改流体种类、数量和 NBT；返回 `null` 或非正数量会取消该输出。
- `.addGasModifier(function(controller, gas))` 可修改气体种类和数量。为兼容可选 Mekanism 依赖，ZenScript 中参数/返回值表现为 `IIngredient`，运行时必须是 `IGasStack`。
- 修改结果会用于输出空间预检、并行数量计算和最终写入。

## 流体预览 NBT 与气体 Tooltip

`.setPreViewNBT(IData)` 可用于物品和流体输入/输出：

```zenscript
recipe
    .addFluidOutput(<liquid:water> * 1000)
    .setPreViewNBT({Potion: "healing"});

recipe
    .addGasOutput(<gas:hydrogen> * 1000)
    .setGasTooltip("纯度：高", "经过压缩处理");
```

流体 NBT 会显示在 JEI 流体槽中，不影响实际匹配或输出。气体不支持 `.setPreViewNBT(IData)`；Mekanism `GasStack` 不支持任意 NBT，气体仅提供 Tooltip API。`.addGasTooltip(String...)` 追加提示行，`.setGasTooltip(String...)` 覆盖此前由本扩展添加的提示行；两者均支持气体输入和输出。

## 脚本设计建议

- 模块 ID 使用稳定的 ASCII `snake_case`，发布后不要随意重命名。
- `moduleList` 适合动态 GUI/诊断，配方判定直接使用 `withModule`/`withoutModule`。
- 不要把 `as-upgrade` 与 `hasModule` 混为一谈：`as-upgrade=false` 只关闭同名升级，不会阻止模块本身生效。
- `addMEChannelInput`、红石注册和控制器红石 API 都应使用稳定名称；发布后修改名称会使旧机器 GUI 标签和脚本不再对应。
