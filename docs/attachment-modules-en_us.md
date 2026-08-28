# MMCE Complement Attachment Modules Guide

[English Wiki overview](wiki-en_us.md) · [中文文档](attachment-modules-zh_cn.md)

This guide is intended for modpack authors. It explains how to add optional multiblock structures, dependencies, conflicts, JEI previews, and recipe conditions to an existing Modular Machinery Community Edition (MMCE) machine.

## Attachment modules in one minute

- The top-level `parts` in a normal MMCE machine JSON still define the main structure. Its reserved ID is `main`.
- A new top-level `modules` array defines zero or more attachment modules. Every module has a unique ID and its own native MMCE `parts`.
- The controller recognizes and forms `main` first, then checks the blocks, dependencies, and conflicts of each attachment module.
- The structures of active modules are added to the machine. Their hatches, buses, selector tags, and other modular blocks participate normally and can use the normal formed-machine color behavior.
- By default, an active module also supplies an MMCE upgrade with the same ID. Set `"as-upgrade": false` to disable that behavior.
- CraftTweaker recipes can require or forbid modules, and scripts can query a controller's module state.

“Declared” and “active” are different states. An ID in `modules` is declared; it becomes active only when its structure matches, every dependency is active, and no conflict disables it.

## Quick start

Starting with an existing `distillation_tower.json`:

1. Keep its original `parts`; those are `main`.
2. Add a top-level `modules` array.
3. Give each module a unique `id` and a `parts` array.
4. Omitting `depends-on` makes the module depend on `main`. Specify parent IDs when building a module chain.
5. Start the game or reload the machine definitions, form the main structure, then build the attachment structures.
6. Cycle through the JEI structure pages to verify coordinates and merged parent previews.
7. If a recipe needs module conditions, add `.withModule(...)` or `.withoutModule(...)` to its RecipeBuilder chain.

Minimal example:

```json
{
  "registryname": "distillation_tower",
  "localizedname": "Distillation Tower",
  "parts": [
    { "x": 0, "y": -1, "z": 0, "elements": ["modularmachinery:blockcasing"] }
  ],
  "modules": [
    {
      "id": "cooling_compressor",
      "parts": [
        { "x": 3, "y": 0, "z": 0, "elements": ["minecraft:iron_block"] }
      ]
    }
  ]
}
```

Do not add a second `main` object to `modules`, and do not put the controller block in a module's `parts`. The controller remains part of MMCE's main machine definition.

## Complete JSON example

The following example demonstrates the default dependency, a dependency chain, a parallel module, conflicts, and disabling the upgrade behavior. Every referenced module is declared, so it can be used directly as a template.

```json
{
  "registryname": "distillation_tower",
  "localizedname": "Distillation Tower",
  "parts": [
    { "x": 0, "y": -1, "z": 0, "elements": ["modularmachinery:blockcasing"] },
    { "x": 0, "y": -2, "z": 0, "elements": ["minecraft:iron_block"] }
  ],
  "modules": [
    {
      "id": "cooling_compressor",
      "parts": [
        { "x": 3, "y": 0, "z": 0, "elements": ["minecraft:iron_block"] },
        { "x": 3, "y": 1, "z": 0, "elements": ["minecraft:packed_ice"] }
      ]
    },
    {
      "id": "spacetime_pipe",
      "depends-on": ["cooling_compressor"],
      "conflicts-with": ["low_pressure_pipe"],
      "parts": [
        { "x": 4, "y": 0, "z": 0, "elements": ["minecraft:diamond_block"] }
      ]
    },
    {
      "id": "low_pressure_pipe",
      "conflicts-with": ["spacetime_pipe"],
      "parts": [
        { "x": -3, "y": 0, "z": 0, "elements": ["minecraft:gold_block"] }
      ]
    },
    {
      "id": "alternate_shell",
      "depends-on": [],
      "as-upgrade": false,
      "parts": [
        { "x": 0, "y": -3, "z": 0, "elements": ["minecraft:glass"] }
      ]
    }
  ]
}
```

## JSON field reference

`modules` must be a top-level array in the machine JSON. Existing machines with no attachment modules do not need this field.

| Field | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `id` | string | Yes | None | A case-sensitive module ID unique within this machine. Surrounding whitespace is trimmed. `main` is reserved. |
| `parts` | array | Yes | None | Uses the same format and coordinate system as the main MMCE `parts`, including MMCE block alternatives, NBT, and component selector tags. |
| `depends-on` | string array | No | `["main"]` | Every parent module that must be active before this module can become active. Use `[]` for no parent in the module graph. |
| `conflicts-with` | string array | No | `[]` | Module IDs incompatible with this module. Conflicts are treated as bidirectional at runtime. |
| `as-upgrade` | boolean | No | `true` | Whether an active module supplies an MMCE upgrade with the same name as its `id`. |

Important rules:

- `parts` must be an array. Complex block definitions must still use MMCE's native `parts` entry format.
- Every ID in `depends-on` and `conflicts-with` must be either `main` or another module declared by the same machine.
- Duplicate IDs, unknown references, empty IDs, self-dependencies, self-conflicts, and dependency cycles cause a machine JSON loading error.
- Duplicate relationships in one array are de-duplicated, but configurations should not rely on that behavior.
- Only the fields in the table should currently be used on a module object. A module does not inherit all top-level settings accepted by a complete machine JSON.

## Coordinates, rotation, and overlapping structures

Module `parts` follow exactly the same coordinate rules as the main structure: the controller is `(0, 0, 0)`, and coordinates rotate with the controller facing. The most reliable approach is to continue using the coordinate system of the main machine JSON.

When a child overlaps a parent, the parent owns the coordinate:

- If the child and any ancestor define different blocks at the same coordinate, the child's definition at that position is ignored. Structure matching, component collection, and JEI preview all use the parent's definition.
- If parent and child define exactly the same block information at a coordinate, they may share that block.
- This rule applies recursively through every `depends-on` ancestor, including `main` when it is the root.

Avoid overlapping modules that have no dependency relationship. Parallel or sibling modules have no explicit parent ownership; give them separate coordinates or add a dependency that makes ownership unambiguous.

`"depends-on": []` only makes a module parallel to `main` in the dependency graph and merged JEI preview. The controller must still recognize and form the main machine before attachment modules are checked.

## Activation, dependency, and conflict rules

Whenever MMCE checks `main` and confirms that the main structure is still formed, the controller resolves modules in the same structure-check pass in this order:

1. Test whether each module's effective `parts` match in the world.
2. Keep only modules whose direct and transitive dependencies are all matched and active.
3. If two structurally matched, dependency-valid modules conflict, neither module becomes active.
4. Children of a disabled conflicting module also become inactive because their dependency is missing.

If `c` depends on `b`, and `b` depends on `a`, all three structures must be present for `c` to become active. Building only `a` and `c` is not enough.

Declaring a conflict on one side is sufficient to make it bidirectional, although declaring it on both modules is recommended for readability. If `a` conflicts with `b` and both would otherwise activate, both are disabled; JSON declaration order does not pick a winner.

When module state changes, the controller requests a new recipe search and updates its structural components. Hatches, buses, and other MMCE modular blocks from active modules join the machine and are removed again when their module becomes inactive.

Attachment matching is change-aware. After a successful check, the expensive module pattern matches are cached while the attachment area is unchanged. A block or chunk update in that area invalidates the cache immediately; a periodic fallback check (100 ticks by default) catches changes made outside the event-listener window. This keeps idle machines from re-matching every attachment on every MMCE structure-check interval without delaying normal build/break updates. If an attachment area is unloaded, the last valid active state is retained until the area can be checked again.

## Modules as MMCE upgrades

`as-upgrade` defaults to `true`. While a module is active, the controller supplies a synthetic MMCE upgrade whose name is the module ID, allowing existing upgrade queries and recipe logic to reuse that ID.

```json
{
  "id": "decorative_shell",
  "as-upgrade": false,
  "parts": [
    { "x": 2, "y": 0, "z": 0, "elements": ["minecraft:glass"] }
  ]
}
```

Setting it to `false` disables only the upgrade behavior. The module is still matched, added to the structure, and available as a dependency of other modules.

## Using the attachment selection tool

The “Attachment Module Area Selection Tool” is available in the MMCE creative tab. Its user must be in Creative mode and have administrator command permission.

It extends MMCE's original `ItemConstructTool`, so:

- Right-clicking normal blocks selects or deselects them exactly like the original tool.
- The white outline highlight is unchanged.
- It uses MMCE's player selection directly and can be alternated with the original tool or other compatible `ItemConstructTool` tools.

### Exporting the first module from a main machine

1. Build the complete main machine and let the controller recognize it.
2. Build the new attachment in the world.
3. Select every new attachment block. It is safe if the selection also includes some main-structure blocks.
4. While holding this tool, sneak-right-click the formed controller.
5. The tool subtracts every coordinate in the controller's currently formed pattern and exports only the remaining blocks.
6. Paste the single module object from the clipboard into the machine JSON's `modules` array, then rename its generated ID.

### Exporting on top of existing modules

Make the existing modules active first, then select and export in the same way. The subtraction includes `main`, every currently active attachment module, and other blocks in the controller's currently formed pattern. A module `2` built on module `1` therefore does not repeat blocks from `main` or module `1`.

The exporter writes every currently active module ID to the new object's `depends-on`. This means all of them are required. If the new module only has one direct parent, simplify the array manually after pasting.

When no attachment module is active, the exported object omits `depends-on`, which loads as the default `["main"]`.

### Export result and saved file

The output is one module object, not a complete machine JSON:

```json
{
  "id": "attachment_20260724_190000",
  "depends-on": [
    "cooling_compressor"
  ],
  "parts": [
    { "x": 4, "y": 0, "z": 0, "elements": ["minecraft:diamond_block"] }
  ]
}
```

It is copied to the client clipboard and saved as `attachment-module-player-moduleID.json` in MMCE's current machinery directory. After pasting it into the real machine JSON, move this standalone export out of the machinery directory or delete it. It is only a module object and cannot be loaded as a complete machine JSON by itself.

If the sneak-right-clicked controller is not formed, the tool falls back to MMCE's original workflow and exports a complete structure JSON. The selection is cleared after every export attempt, successful or not.

## JEI structure preview

Machines with `modules` receive two controls in the bottom-right area of the original MMCE structure preview:

- Module switch button: cycles through `main` and then every module in JSON declaration order. Its tooltip shows the current module.
- Merge with parents button: appears to the left of the switch button only when the selected attachment has a parent. It is off by default.

The `main` page shows the original structure. A module-only page shows that module's effective blocks and keeps the main controller as a positional reference. Enabling the merge button displays the selected module together with all direct and transitive parents up to the dependency root, usually `main`.

JEI displays module definitions and parent-overlap rules. It does not indicate whether that module is currently active in a particular world machine.

## CraftTweaker recipe conditions

The mod expands `mods.modularmachinery.RecipePrimer` with:

```zenscript
withModule(string[] ids) as RecipePrimer
withoutModule(string[] ids) as RecipePrimer
```

- `withModule`: every ID in the array must be active.
- `withoutModule`: the recipe is blocked if any ID in the array is active.
- Calls may be repeated. Conditions accumulate, and duplicate IDs are de-duplicated.
- A module ID cannot be empty or the reserved ID `main`.
- The same recipe cannot both require and forbid one module; recipe creation throws an error in that case.

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

Module state is resolved only by the machine structure-check task. The same structure pass precomputes an immutable result for every module-restricted recipe of that machine. Recipe start, restart, and running ticks only read that result; they no longer copy the active-module set or perform module formation checks. Losing a required module or gaining a forbidden module returns the corresponding crafting failure after the structure check refreshes the snapshot.

## CraftTweaker controller queries

The mod also expands `mods.modularmachinery.IMachineController`:

```zenscript
// Whether this attachment module is currently active.
val coolingActive as bool = controller.hasModule("cooling_compressor");

// All modules declared by the recognized machine, in JSON order and without main.
val declaredModules as string[] = controller.moduleList;
```

| Member | Returned value |
| --- | --- |
| `hasModule(string)` | Whether the specified module is currently active. Inactive, undeclared, and `main` all return `false`. |
| `moduleList` | Every attachment ID declared by the currently recognized machine, not the active-module list. Returns an empty array before a machine is recognized. |

To obtain active module IDs, iterate through `moduleList` and test each ID with `hasModule`.

## Recommended design patterns

- Linear upgrade chain: `main <- cooling <- compression <- spacetime`, with each level depending only on its direct parent.
- Multiple prerequisites: `"depends-on": ["cooling", "power_booster"]` means both modules are mandatory.
- Mutually exclusive variants: make both depend on `main` and list each other in `conflicts-with`.
- Parallel appearance: use `"depends-on": []`; also set `"as-upgrade": false` when it should not affect upgrade logic.
- Avoid listing every currently active module as a permanent parent. Precise direct dependencies make JEI merged previews and future extensions easier to understand.

## Troubleshooting

### The machine JSON does not load

Look for an attachment-module error in the log and verify that:

- `modules` is a top-level array and every entry is an object.
- Every object has a string `id` and an array `parts`.
- There is no `main` ID, duplicate/empty ID, self-dependency, self-conflict, or dependency cycle.
- Every `depends-on` and `conflicts-with` reference is declared by the same machine.
- Standalone files produced by the selection tool have been moved out of the machinery directory.

### The blocks are built, but the module is inactive

- Confirm that the main machine is formed and faces the expected direction.
- Compare relative coordinates against the JEI module-only page; coordinates rotate with the controller.
- Confirm that every parent is built and active, not merely physically present.
- Check for another matched conflicting module. A conflict disables both sides.
- Inspect positions overlapping a parent. A differing child definition is ignored there, and the parent supplies the required block.

### The selection tool exports no attachment blocks

- Use the “Attachment Module Area Selection Tool” in Creative mode with administrator permission.
- Sneak-right-click the controller and ensure the selection contains at least one new block outside the currently formed pattern.
- A selection containing only `main` and active modules becomes empty after subtraction and cannot be exported.
- To subtract an existing attachment automatically, make sure that attachment is actually active first.

### Recipe conditions behave unexpectedly

- `withModule(["a", "b"])` is AND: both `a` and `b` must be active.
- `withoutModule(["a", "b"])` is a forbidden set: either active module blocks the recipe.
- `moduleList` is the declaration list; use `hasModule` for runtime state.
- IDs are case-sensitive and must exactly match the machine JSON.
