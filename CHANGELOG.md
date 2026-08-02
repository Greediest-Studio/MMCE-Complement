# Changelog

## [1.3.1] - 2026-08-02

### 修复

- 修复批处理仓 Mixin 覆盖 MMCE 原生并行计算结果的问题。
- 修复通过 ZenScript、配方检查事件或其它模组调用 `ActiveMachineRecipe#setMaxParallelism` 强制设置的配方并行数失效的问题。
- 修复 MMCE 并行控制器与其它运行时机器并行修改可能被构造时缓存值覆盖的问题。
- 修复重复执行配方检查时，批处理倍率可能在已经批处理过的并行数上继续叠乘的问题。
- 修复批处理仓处理未启用原生并行的配方时，运行时并行预算可能被强制压回 `1` 的问题。

### 兼容性调整

- 当前检查周期中由 MMCE、ZenScript、并行控制器及其它 Mixin 得到的 `maxParallelism` 现在被视为权威基础值。
- 批处理仓只在该基础值上追加自身负责的并行倍率，不再重建或替换原有并行预算。
- 没有生效的批处理仓时，本模组不再写入或修正 `maxParallelism`。
- 工厂的特殊自定义线程仍不会被批处理仓额外放大。

### 测试

- 增加运行时并行覆盖、直接字段修改、显式 setter 覆盖及重复批处理检查的回归测试。
- 通过完整单元测试及生产环境 reobf 构建。

> 建议所有使用 ZenScript 动态并行、MMCE 并行控制器或第三方并行扩展的整合包更新至本版本。本次更新不新增配置项，也不需要迁移机器 JSON 或存档数据。

## [1.0.0] - 2023-09-15

### Added
- This is a default template changelog that follows the [KeepAChangelog Convention](https://keepachangelog.com/en/1.1.0/)
