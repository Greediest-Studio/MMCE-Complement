# MMCE Complement Wiki (English)

This page documents MMCE Complement **1.4.1** for Minecraft 1.12.2.

[中文 Wiki](wiki-zh_cn.md) · [Attachment Modules Guide](attachment-modules-en_us.md)

## About

MMCE Complement is an add-on for [Modular Machinery Community Edition (MMCE)](https://www.curseforge.com/minecraft/mc-mods/modular-machinery-community-edition). It adds machine hatches, item/fluid/gas assemblies, AE2 network bridges, named redstone data interfaces, attachment modules, and CraftTweaker extensions for modpack authors.

## Installation and optional dependencies

Required gameplay dependencies are Minecraft Forge, MMCE, and AE2 Extended Life. MixinBooter remains a required technical loader. Every other integration is optional to this addon and is registered only when its mod and required API classes are present. GeckoLib may still be required transitively by the installed MMCE build, but MMCE Complement no longer declares it as its own hard dependency.

| Feature | Additional optional dependency |
| --- | --- |
| ME item buses, ME Machinery Pattern Provider II, ME Channel Input Hatch | None beyond required AE2/MMCE |
| ME energy input/output buses | CrazyAE |
| ME mana input/output buses | CrazyAE + the Modular Magic API bundled with MMCE |
| ME gas buses and mixed gas assemblies | Mekanism + Mekanism Energistics |
| Fake fluid/gas ingredients in Pattern Provider II | AE2 Fluid Crafting Rework (plus Mekanism Energistics for gas) |
| Wireless Flux hatches | Flux Networks |
| Recipe and controller scripts | CraftTweaker 2 |
| Recipe displays | JEI or HEI, as provided by the MMCE environment |
| Infusion-registry compatibility fix | Astral Sorcery + Nova Engineering (activated when the adapter is present) |

All registry names use the `mmce_complement:` namespace. When an optional dependency is absent, its related IDs are not registered.

## Core hatches and assemblies

### Performance hatches

- **Thread Hatch** (`thread_hatch`), MK I–VI, raises the machine's normal thread limit. Default multipliers are 2, 3, 5, 8, 12, and 16 and can be configured. By default only the highest installed tier applies; `allow_stacking=true` multiplies all installed hatches. Special custom threads are not increased.
- **Overclock Hatch** (`overclock_hatch`), MK I–VI, increases energy use while shortening recipe duration. Multipliers are configurable; only the highest tier applies unless stacking is enabled.
- **Accelerator Hatch** (`accelerator_hatch`), MK I–VIII, unconditionally shortens recipe duration. Only the highest tier applies in one machine.
- **Batch Hatch** (`batch_hatch`) sets a maximum batch duration in its GUI and trades processing time for higher per-batch parallelism. If several are installed, the largest configured duration wins.

### Multi-tank fluid hatches

- **Quadruple Fluid Input/Output Hatch** (`quad_fluid_input_hatch_tiny`, `quad_fluid_output_hatch_tiny`) follows MMCE's eight size tiers (Tiny through Vacuum) and has four isolated tanks. Input tanks reject duplicate fluids; output tanks may contain the same fluid in different slots.
- **Ninefold Fluid Input/Output Hatch** (`nine_fluid_input_hatch_normal`, `nine_fluid_output_hatch_normal`) has six tiers (Normal through Vacuum) and nine isolated tanks. Input tanks reject duplicates; output tanks allow them.
- With Mekanism installed, these hatches and the normal input/output assemblies can also carry Mekanism gases. Fluid amounts are shown in mB.

Version 1.4.0 fixes the save/load bug that changed every quadruple-hatch tank to 25 mB after reloading a world. The hatch tier is now persisted and the correct per-tank capacity is restored.

### Item, fluid, and data assemblies

`data_input_assembly_hatch`, `input_assembly_hatch`, `output_assembly_hatch`, and `self_cycle_assembly_hatch` each have Small, Normal, Big, Huge, and Ludicrous tiers. Exact item-slot and fluid capacities are shown in tooltips and can be configured.

- **Data Input Assembly** accepts items, fluids (and Mekanism gases) and also exposes MMCE's smart data interface. Its value takes priority for data checks in the same recipe group.
- **Input Assembly** accepts normal item, fluid, and gas inputs without the data interface.
- **Output Assembly** buffers machine products for output; different fluid slots may contain the same fluid.
- **Self-Cycle Assembly** preferentially returns matching inputs actually consumed from itself, for recipes that circulate a medium. Other products are not inserted into it.

Configure per-tank capacities in `config/mmce_complement.cfg` under `data_input_assembly` and `input_assembly` (mB).

### Other hatches

- **Liquid Energizer Hatch** (`liquid_energizer_hatch`) converts configured fluids directly into machine energy using `fluid_registry_name=energy_per_mB` ratios. Per-tier fluid and energy capacities are configurable.
- **Filtered Item Output Hatch** and **Filtered Fluid Output Hatch** (`filtered_item_output_hatch`, `filtered_fluid_output_hatch`) provide one filtered output slot/tank with a capacity of 2,147,483,647; matching recipe products enter them first.
- **Machine Glass** (`machine_glass`) and the **Machine Casings** (`blockcasing`) are used for machine structures and appearance.

## ME network features

ME blocks require a powered, valid AE2 network. Unless noted otherwise, drag an ingredient from JEI into a configuration slot to mark it.

### ME energy and mana buses

| Registry ID | Name | Function |
| --- | --- | --- |
| `me_energy_input_bus` / `me_energy_output_bus` | ME Mechanical Energy Input/Output Bus | Transfers machine energy through the CrazyAE energy channel |
| `me_mana_input_bus` / `me_mana_output_bus` | ME Mechanical Mana Input/Output Bus | Transfers mana through the CrazyAE/Modular Magic mana channel |

Both families expose configurable buffer capacity and show the current buffer and channel cost in their GUIs; actual movement is governed by the corresponding AE storage channel and machine-side capacity.

### ME inventory input buses

These four buses expose 16 configuration slots and share the Ore Dictionary bus's active-pulling button style:

| Registry ID | Name | Marker |
| --- | --- | --- |
| `me_ore_dict_input_bus` | ME Mechanical Ore Dict Input Bus | Ore-dictionary expressions |
| `me_item_inventory_input_bus` | ME Mechanical Item Inventory Input Bus | Items |
| `me_fluid_inventory_input_bus` | ME Mechanical Fluid Inventory Input Bus | Fluids |
| `me_gas_inventory_input_bus` | ME Mechanical Gas Inventory Input Bus | Gases (requires Mekanism Energistics) |

Rules shared by the buses:

1. Marker amounts are always displayed as **1** and cannot be edited. The value means “this type is marked,” not an extraction amount.
2. With **Active pulling** enabled, each marker pulls all currently available stock of that type from the AE network, subject to slot capacity and the permanent reserve.
3. With Active pulling disabled, markers remain configured but the bus is in **Standby** and does not pull. Buffered contents are returned to the network when possible.
4. **Permanent stock reserve** defaults to 0. Items use item counts; fluids and gases use mB. Extraction never lowers the network below the configured value. For example, with 32 iron ingots and a reserve of 16, at most 16 are pushed to the machine.
5. The Ore Dictionary bus additionally supports whitelist/blacklist expressions. In active mode it automatically selects and maintains up to 16 matching types; in standby mode markers can be kept manually.

### ME mechanical assemblies

- **ME Mechanical Input Assembly** (`me_input_assembly`) has 16 mixed slots. Each slot can represent an item, fluid, or gas and contributes to one machine input group. Fluids and gases render at normal slot size rather than as stretched bars.
- **ME Mechanical Inventory Input Assembly** (`me_inventory_input_assembly`) adds Active pulling and permanent-reserve controls to the mixed input assembly.
- **ME Mechanical Output Assembly** (`me_output_assembly`) has 16 mixed output slots and automatically inserts machine products into the AE network.
- **ME Mechanical Full Exposure Assembly** (`me_full_exposure_assembly`) has no filter. When Active pulling is enabled it pulls all items, fluids, and gases from the current AE network, showing at most 16 entries; disabling it stops pulling and pushes buffered contents back.

Assemblies use `§b` dynamic coloring to distinguish them from ordinary buses; their localized names are not independently colorized.

### ME Machinery Pattern Provider II

The registry ID is `me_pattern_provider_ii`. It keeps MMCE's network and memory-card contracts while expanding the provider to:

- 144 pattern slots in an **8 × 18** layout;
- **9 item slots + 3 fluid slots** in the lower-right independent storage area, with an `F` marker on fluid slots;
- the same GUI controls, buttons, work modes, hand-swing animation, and empty-hand sneak-right-click group settings operation as the MMCE provider;
- the vanilla actual-inventory count display, without an extra misleading pattern tooltip;
- the original Default, Blocking, Crafting Lock, Enhanced Blocking, and Enhanced Isolation Input modes;
- compatibility with MMCE's **ME Mechanical Pattern Mirror** and Whimcraft 0.1.4's **ME Mechanical Pattern Provider Inventory Sharing Bus**. The sharing bus shares the public item/fluid handler; it does not split the 144 pattern slots or the isolated per-pattern inventories.

## ME Channel Input Hatch

**ME Channel Input Hatch** (`me_channel_input_hatch`) connects directly to AE2 ME cables. It consumes no channel while idle and requests channels dynamically only while a recipe is running.

- Recipe demand must be a positive integer.
- Before starting, the machine checks whether all of its channel hatches can provide the requested total on one AE network; shared cable capacity is counted once.
- The requested channels remain reserved for the recipe and are released when it completes, is cancelled, or the machine is removed.
- A network recalculation, disconnect, controller conflict, or an in-progress drop below the requirement pauses the recipe and reports the corresponding error.
- JEI includes a dedicated channel icon and amount renderer.

CraftTweaker recipe example:

```zenscript
// Require 8 allocatable ME channels
mods.modularmachinery.RecipeBuilder.newBuilder(
    "channel_recipe", "my_machine", 200
)
    .addMEChannelInput(8)
    // Add normal inputs, outputs, energy, and duration as usual
    .build();
```

## Redstone hatches and data interfaces

### Redstone Control Hatch

**Redstone Control Hatch** (`redstone_control_hatch`) is an actively connectable redstone machine controller. It reads redstone power delivered to **itself**; when the signal is greater than or equal to the shutdown threshold, the machine pauses as if its controller were powered.

- Right-click: increase the threshold by 1, cycling from 15 back to 1, and show the value in the status bar.
- Shift + right-click: reset the threshold to 1.
- The block has on/off overlays and follows the formed machine's color.

### Redstone Signal Input/Output Hatches

These named data interfaces (`redstone_signal_input_hatch`, `redstone_signal_output_hatch`) connect a machine to external redstone circuitry and both support redstone connections.

Register machine-scoped values with CraftTweaker:

```zenscript
import mods.mmce_complement.RedstoneInterface;

// Default operator=0: maximum of same-name input hatches
RedstoneInterface.newRedstone("mymod:my_machine", "temperature").build();
// operator=1: minimum; operator=2: sum of all same-name input signals
RedstoneInterface.newRedstone("mymod:my_machine", "pressure", 1).build();
RedstoneInterface.newRedstone("mymod:my_machine", "throughput", 2).build();
```

| `operator` | Aggregation |
| --- | --- |
| `0` (default) | Maximum of all same-name redstone input signals |
| `1` | Minimum |
| `2` | Sum of all same-name input signals |

Read and write values from the controller event script:

```zenscript
val current = controller.getRedstone("temperature");
controller.setRedstone("throughput", 15); // clamped to 0–15
```

An input hatch reads its local signal and aggregates same-name hatches using the registered operator. An output hatch emits the value assigned by the script during the current machine event tick; its GUI-selected name must match the script name. Unknown names return 0 and duplicate registrations are logged as CraftTweaker errors.

## Wireless Flux hatches

With Flux Networks installed:

- `flux_input_hatch` receives energy from a wireless Flux network;
- `flux_output_hatch` sends energy to a wireless Flux network.

Both support Flux Networks' network ID, name, priority, surge/disable-limit, and chunk-loading settings. Version 1.4.0 defaults are:

- Internal buffer capacity: **10,000 FE**;
- Default transfer rate: **800,000 FE/t**.

## CraftTweaker and attachment-module APIs

See the [Attachment Modules Guide](attachment-modules-en_us.md) for the JSON format, dependencies/conflicts, JEI previews, and structure export. Common script extensions are:

```zenscript
// Require or forbid attachment modules
recipe.withModule(["cooling_compressor"]);
recipe.withoutModule(["low_pressure_pipe"]);

// Query active modules on a controller
if (controller.hasModule("cooling_compressor")) {
    // The module is active
}
val modules = controller.moduleList;
```

`addMEChannelInput(int)`, `RedstoneInterface.newRedstone(...).build()`, `controller.getRedstone(...)`, and `controller.setRedstone(...)` have an effect only when the corresponding integration is available.

Attachment structure checks run synchronously with MMCE's main structure check, using the same schedule and thread. There is no independent per-tick attachment scan or change-listener polling path.

## 1.4.0 summary

- Added unified marker, active-pulling, and permanent-reserve behavior to the ME item/fluid/gas/ore-dictionary inventory input buses.
- Added the mixed ME Input, Inventory Input, Output, and Full Exposure Assemblies.
- Added the ME Channel Input Hatch, JEI support, dynamic channel reservations, and CraftTweaker `.addMEChannelInput(int)`.
- Added the Redstone Control, Redstone Signal Input, and Redstone Signal Output Hatches with named redstone-interface APIs.
- Added the 144-slot ME Machinery Pattern Provider II with MMCE pattern-mirror and Whimcraft inventory-sharing compatibility.
- Updated Wireless Flux Hatch defaults to a 10,000 FE buffer and 800,000 FE/t transfer rate.
- Fixed quadruple-fluid save/load capacity, Pattern Provider II GUI/material/interaction issues, ME assembly textures, and Redstone Control Hatch connection/signal handling.
- Fixed Nova Engineering's optional Astral Sorcery compatibility path, which could query the infusion registry before it had been compiled and cause a startup exception.

## Troubleshooting

**ME or gas blocks are missing.** Verify AE2 Extended Life, CrazyAE, Mekanism, and Mekanism Energistics as appropriate; blocks are registered dynamically from their dependencies.

**An ME channel recipe will not start.** Ensure every ME Channel Input Hatch is on the same powered AE network, wait for channel recalculation to finish, and verify enough channels are allocatable.

**An inventory input bus does not pull.** Set Active pulling to Enabled; disabled buses are in Standby. A permanent reserve equal to or above the network stock intentionally makes extraction return zero.

**A fluid hatch shows the wrong capacity.** Break and replace a block created by an older version or let the chunk save again. Version 1.4.0 persists the block tier and no longer resets each tank to 25 mB.
