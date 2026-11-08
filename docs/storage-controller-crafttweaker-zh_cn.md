# 存储控制器 CraftTweaker API

脚本类型为 `mods.modularmachinery.IMachineStorageController`，继承 MMCE 的 `IMachineController`。适用于本模组的多方块存储控制器；普通配方机器不能转换为此类型。AE2UEL 与 AE2S 使用同一套 API。

## 获取接口

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

## 配置和可用性

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

## 动态修改槽数和容量

以下方法均返回 `bool`，仅在 `storageAvailable` 为 true 时执行。修改只影响当前这台机器，
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
- 缩减槽数时，所有被删除的末尾槽位必须为空；不会自动搬移、丢弃或掉落其内容。
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

多个设置调用各自独立，并非一组事务。降级脚本应检查每个返回值，先取走末尾槽位和超出目标容量的内容，再尝试缩容。

## 查询和存取

下表中的 X 替换为 `Item`、`Fluid` 或 `Gas`。对应的参数类型分别为 `IItemStack`、`ILiquidStack`、`IIngredient`；气体必须使用 Mekanism 的真实气体栈，例如 `<gas:hydrogen>`，不支持矿辞、通配符或组合材料。

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `hasX(stack)` | bool | 是否存在至少一个/一单位匹配内容，忽略请求数量 |
| `countX(stack)` | long | 所有槽位中匹配内容的总量 |
| `insertX(stack, simulate = false)` | int | 实际或模拟可添加的数量 |
| `extractX(stack, simulate = false)` | int | 实际或模拟可提取的数量 |
| `insertXInSlot(index, stack, simulate = false)` | int | 仅向指定分类槽位添加 |
| `extractXInSlot(index, stack, simulate = false)` | int | 仅从指定分类槽位提取 |
| `getXInSlot(index)` | 对应的栈或 null | 指定分类槽位的内容快照 |

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

## 槽位索引和唯一编号

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

## 继承接口的行为

位置、世界、朝向、自定义数据、机器模式、结构查询等原接口照常可用。存储控制器不执行配方，因此：

- `activeRecipe` 返回 null。
- `activeRecipeList`、`recipeThreadList` 返回空数组。
- `isWorking` 返回 false；判断存储可用性使用 `storageAvailable`。
- `extraThreadCount` 返回 0，写入忽略。
- `addModifier`、`removeModifier`、`addPermanentModifier`、`removePermanentModifier` 无操作；`hasModifier` 返回 false。

