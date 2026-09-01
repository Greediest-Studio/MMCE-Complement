# CraftTweaker 流体输出修改器

MMCE Complement 为 `mods.modularmachinery.RecipePrimer` 增加了
`.addFluidModifier()`。它必须紧跟在一个流体输出之后，并接收一个
`mods.modularmachinery.AdvancedFluidModifier` 函数：

```zenscript
mods.modularmachinery.RecipeBuilder.newBuilder(
    "dynamic_fluid", "my_machine", 200
)
    .addFluidOutput(<liquid:water> * 1000)
    .addFluidModifier(function(controller, liquid) {
        // 将每次输出的 1000 mB 水改为 2000 mB 熔岩。
        return <liquid:lava> * (liquid.amount * 2);
    })
    .build();
```

函数签名为：

```zenscript
function(IMachineController controller, ILiquidStack liquid) as ILiquidStack
```

- 返回堆的流体种类、数量和 NBT 都会成为实际输出。
- 多次调用 `.addFluidModifier()` 时，函数按声明顺序依次执行。
- 返回 `null` 或数量小于等于零的流体堆会取消该项流体输出。
- 修改后的内容和数量同时用于配方开始前的输出空间检查与完成时的实际写入。
- 普通 `RecipeModifier`、配方并行数和适配器配方仍会共同生效；并行输出总量会以修改后的单次输出量为基础计算。

该方法只接受流体输出；如果前一个配方组件不是流体输出，
CraftTweaker 日志会给出警告且不会添加修改器。
