# 流体预览 NBT 与气体 Tooltip

MMCE Complement 使用 MMCE 原版的 `.setPreViewNBT(IData)` 方法，并将其扩展到流体组件。
该方法可用于物品和流体输入/输出：

```zenscript
recipe
    .addFluidOutput(<liquid:water> * 1000)
    .setPreViewNBT({Potion: "healing"});

recipe
    .addGasOutput(<gas:hydrogen> * 1000)
    .setGasTooltip("纯度：高", "经过压缩处理");
```

- 流体：NBT 作为流体显示 NBT 写入 JEI 预览，不改变实际输入匹配或输出内容。
- 气体不支持 `.setPreViewNBT(IData)`；Mekanism `GasStack` 不支持任意 NBT，气体仅提供 Tooltip API。
- `.addGasTooltip(String...)` 会追加自定义 JEI 气体提示行；`.setGasTooltip(String...)`
  会先清除之前添加的自定义行再写入新内容。自定义行不影响实际气体数据。
- 两者都支持输入和输出组件，并会在配方深拷贝/适配器配方中保留。
- 预览 NBT 仅适用于物品和流体；气体请使用 Tooltip 方法。
