# AE2S 安装产物验收

Minecraft 1.12.2 / **Cleanroom 0.6.8+ / Java 25**，使用真实 SRG 安装 JAR。
MMCE、AE2S、AppliedCompatibility 均为未修改的官方发行包。
完整依赖和支持边界见 [兼容说明](../docs/AE2S-COMPATIBILITY.md)。

- dependencies.json：六个官方运行依赖的来源与 SHA-256。
- dependencies-gas.json：增加 Mekanism / MekEng / CEu AE Upgrade 的气体组合。
- mmce-2.3.3-sources.json：2.3.3 源码、签名核对及未找到发布 JAR 的记录。
- launch.py：校验官方安装器和运行库，启动真实客户端或专服。
- run.py：校验依赖锁，安装 Complement / 独立验收模组及 JSON 结构夹具。
- machinery/：真实存储多方块和连接共享结构。
- reports/README.md：分版本结果、产物与前置哈希，以及输出总成修复截图。
- evidence/：本地原始日志、截图和历史试验，不提交到仓库。

构建 reobfJar reobfRuntimeHarnessJar 后，在本工作区运行：

    python3 verification/run.py server --name mmce-recheck-server --dependencies /path/to/jars
    DISPLAY=:97 LIBGL_ALWAYS_SOFTWARE=1 python3 verification/run.py client --name mmce-recheck-client --dependencies /path/to/jars
    python3 verification/run.py server --name mmce-recheck-0714-server --cleanroom-version 0.7.14-alpha --dependencies /path/to/jars

气体场景在命令中追加 `--dependency-lock verification/dependencies-gas.json`，
并使用包含该锁中九个官方 JAR 的依赖目录。

默认基线为官方 0.6.8-alpha，较新版本使用 --cleanroom-version 0.7.14-alpha。
0.6.13-alpha 也可选择，气体组合已通过该版本的客户端与专服功能检查。
启动器使用 Foundation 默认类加载方式；旧脚本的 `foundation.loadsall=true`
会在较新加载器触发 `ft` 错误，已移除，历史失败单独保留。

MMCE 2.3.3 的可下载发布 JAR 尚未找到，当前基础/气体锁均准确使用 2.3.2。
监听器的 2.3.3 `Consumer<Integer>` 桥按已下载源码测试，不替代整个 2.3.3 的运行验收。
场景名必须独立，端口可用 --port 指定。
客户端需要有效 X11 显示；可通过 AE2_ADDONS_JAVA_HOME 指定 Java 25。
脚本当前针对 Linux x64，使用工作区的 Minecraft 资源和元数据缓存。

runtime.json 记录实际模组、安装器和运行库哈希；verification-report.json 由游戏中的
独立 harness 输出；result.json 只有在报告通过、加载器版本匹配、进程正常退出且没有节点销毁异常时
才为 FUNCTIONAL_PASS。只达到专服 Done 的结果为 STARTUP_PASS。
src/runtimeHarness/ 单独打包，不能放进正常游戏发布包。
