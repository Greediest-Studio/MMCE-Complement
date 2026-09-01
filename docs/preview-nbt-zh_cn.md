# 流体/气体预览 NBT

MMCE Complement 统一使用 MMCE 原版的 `.setPreViewNBT(IData)` 方法，并将其扩展
到流体和气体组件。该方法现在可用于物品、流体和气体输入/输出：

```zenscript
recipe
    .addFluidOutput(<liquid:water> * 1000)
    .setPreViewNBT({Potion: "healing"});

recipe
    .addGasOutput(<gas:hydrogen> * 1000)
    .setPreViewNBT({mode: "high_purity"})
    .setGasTooltip("纯度：高", "经过压缩处理");
```

- 流体：NBT 作为流体显示 NBT 写入 JEI 预览，不改变实际输入匹配或输出内容。
- 气体：Mekanism `GasStack` 不支持任意 NBT，因此 NBT 仅作为预览元数据显示在
  JEI 气体槽提示中，不会改变气体匹配或输出。
- `.addGasTooltip(String...)` 会追加自定义 JEI 气体提示行；`.setGasTooltip(String...)`
  会先清除之前添加的自定义行再写入新内容。自定义行不影响实际气体数据。
- 两者都支持输入和输出组件，并会在配方深拷贝/适配器配方中保留。
- 不再提供单独的 `.setPreviewNBT(IData)` 拼写；请统一使用 `.setPreViewNBT(IData)`。
