# 多方块存储器

适用版本：MMCE Complement **1.6.2**、Minecraft 1.12.2。

存储器复用 MMCE 的结构匹配、组件和附属模块，控制器负责保存物品、流体及可选 Mekanism 气体。
它不运行配方、不搜索配方，不需要用空配方维持工作。打开控制器可切换存储类型、滚动查看槽位和整理内容。

## 定义机器

在机器 JSON 顶层加入 `"variant": "storage"`，推荐用 `storage` 对象声明容量：

```json
{
  "registryname": "example_storage",
  "localizedname": "示例多方块存储器",
  "variant": "storage",
  "storage": {
    "slots": 108,
    "fluid": { "slots": 9, "capacity": 16000 },
    "gas": { "slots": 9, "capacity": 64000 }
  },
  "parts": [
    { "x": 0, "y": -1, "z": 0, "elements": ["modularmachinery:blockcasing"] },
    { "x": 1, "y": 0, "z": 0, "elements": ["mmce_complement:storage_interaction_assembly"] },
    { "x": -1, "y": 0, "z": 0, "elements": ["mmce_complement:ae_storage_access_port"] }
  ]
}
```

示例声明物品 108 槽、流体 9 槽（每槽 16,000 mB）、气体 9 槽（每槽 64,000 Mekanism 单位）。
气体需要 Mekanism；不使用气体时可删除 `gas`。流体同理。存储对象本身也会启用存储变体，显式写 `variant` 便于阅读。

| 字段 | 说明 |
| --- | --- |
| `variant` | `storage` 表示存储型机器 |
| `storage.slots` | 物品槽数；默认 54，JSON 中小于 1 时回到 54，超过 4096 时限制为 4096 |
| `storage.fluid.slots` / `storage.gas.slots` | 对应类型的槽数；与容量同时为正才开启 |
| `storage.fluid.capacity` / `storage.gas.capacity` | 每个槽的容量，分别为 mB / Mekanism 原始单位 |

启动后使用这台机器对应的**存储控制器**，注册路径为 `<机器路径>_storage_controller`，
沿用机器命名空间；不要凭名称猜成普通的 `<机器路径>_controller`。
机器需要在注册阶段加载，新增定义后应完整重启。

## 接入自动化

| 方式 | 方向与操作 |
| --- | --- |
| 普通输入仓/输入总成 | 成型后将其缓存内容移入控制器存储 |
| 普通输出仓/输出总成 | 成型后将控制器存储移入输出缓存 |
| 存储交互总成 | 将控制器的实际库存直接暴露给管道，无独立缓存，无 GUI，不连接 ME 导线 |
| ME 存储访问端口 | 直接向有电的 ME 网络暴露库存，提供原生存储总线设置 GUI |
| CraftTweaker | 在服务端机器事件查询、存取和修改容量 |

两种直接接口的方块 ID 和区别见 [存储访问接口](ae-storage-access-port-zh_cn.md)。
输入/输出仓要属于已识别的主结构或生效附属模块；仅贴在结构旁边不会加入机器。

同一机器的控制器、直接接口和脚本访问同一份后端。增加接口不会增加容量，
同一存储器在同一 ME 网络中通过多个端口接入时不会重复统计库存。

## 存档与拆除

临时拆坏外壳会封存库存，管道、AE 和脚本无法继续访问；重建后恢复。
拆掉接口不会将其当成独立仓库复制库存。控制器本身保存内容；破坏控制器会掉落物品库存，
不应把破坏控制器当作搬运流体/气体的手段，应先转移内容。

控制器内的动态容量设置会随 NBT 保存，使用方法见
[存储控制器 CraftTweaker API](storage-controller-crafttweaker-zh_cn.md)。
未成型时仍可查询已配置容量，但不能存取或修改容量。

## AE2UEL 与 AE2S

AE2UEL 与 AE2S 共用机器定义、存储交互总成和脚本 API。
ME 存储访问端口分别使用各自的存储总线实现。AE2S 需要 Cleanroom、Java 25 和
AppliedCompatibility；ME 气体访问还需该分支对应的 MekEng，不能混装两种 AE 主模组。
依赖与实际验证范围见 [AE2S 兼容说明](AE2S-COMPATIBILITY.md)。
