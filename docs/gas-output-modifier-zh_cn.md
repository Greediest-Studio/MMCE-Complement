# CraftTweaker 气体输出修改器

安装 Mekanism 时，MMCE Complement 为
`mods.modularmachinery.RecipePrimer` 增加 `.addGasModifier()`。它必须紧跟
在一个气体输出之后：

```zenscript
mods.modularmachinery.RecipeBuilder.newBuilder(
    "dynamic_gas", "my_machine", 200
)
    .addGasOutput(<gas:hydrogen> * 1000)
    .addGasModifier(function(controller, gas) {
        // 将每次输出的氢气改为双倍数量的氧气。
        return <gas:oxygen> * (gas.amount * 2);
    })
    .build();
```

函数类型是 `mods.modularmachinery.AdvancedGasModifier`。为了保持没有
Mekanism 时的可选依赖兼容性，其气体参数和返回值在 ZenScript 类型系统
中表现为 `IIngredient`，运行时实际值必须是 Mekanism `IGasStack`。

- 返回气体的种类和数量都会成为实际输出。
- 多个 `.addGasModifier()` 按声明顺序执行。
- 返回 `null`、非气体值或数量小于等于零会取消该项气体输出。
- 修改结果同时用于输出空间预检和配方完成时的实际写入。
- 普通 `RecipeModifier`、配方并行数及适配器配方仍会共同生效。

如果前一个组件不是气体输出，CraftTweaker 会记录警告且不添加修改器。
