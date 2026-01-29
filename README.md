# Yoink Bombs

Explode it. Break it. Yoink the loot.

Yoink Bombs is a Hytale mod that adds area block destruction and an auto-loot ("yoink") effect: dropped items are sent to the player who triggered the explosion. Use bombs by throwing them or by breaking a block with a bomb in hand; use the bow or crossbow to fire bomb arrows. All items are Legendary and craftable at the Workbench (Workbench_Tools).

---

## Behaviour

### How yoink works

- **Block-break path:** When you break a block with a Yoink Bomb in hand (right-click / use), the mod collects blocks in a radius (or connected ore/crops for Ore/Harvester), breaks them, and either adds drops to your inventory or spawns them at your feet.
- **Thrown / shot path:** When a bomb or bomb arrow explodes, the game breaks blocks in the explosion radius. The mod then **yoinks** item drops near the explosion into your inventory (or spawns remainder at your feet). You must have a Yoink Bombs item in hand (any bomb, any arrow, bow, or crossbow) when the projectile is removed for yoink to run.
- **Fallback pull:** If you have the **Yoink Bombs Bow** or **Crossbow** equipped, the mod also pulls items within a large radius (default 32 blocks, configurable) around you every tick. This works even when the explosion yoink does not run.

### Who can use it

- Only players with the **yoinkbombs.trusted** permission can trigger block-break yoink (breaking blocks with a bomb in hand).
- Thrown bombs and bomb arrows explode and break blocks per game rules; yoink still requires a Yoink Bombs item in hand when the projectile is removed.

### Item quality and crafting

- All Yoink Bombs items (bombs, arrows, bow, crossbow) are **Legendary** and have **crafting recipes** at the **Workbench** (Workbench_Tools). See [Recipes](#recipes) below.

---

## Features

- Area block destruction (configurable radius per variant)
- Auto-loot: drops go to inventory when possible, remainder at player feet
- Bow and crossbow: fire bomb arrows; fallback pull when bow/crossbow equipped
- Server-safe: respects claims / protection; configurable; permission-gated

---

## Bomb and arrow variants

### Area Bomb

- Explosion destroys nearby blocks
- All resulting drops are teleported to the triggering player
- Thrown bombs stick to surfaces before exploding

### Ore Bomb

- Thrown directly at ore blocks to mine them
- Only targets ore blocks within a small radius
- All resulting drops are teleported to the triggering player
- Thrown bombs stick to surfaces before exploding

### Harvester Bomb

- Harvests crops only (blocks with IDs containing "crop" or "plant")
- All resulting drops are teleported to the triggering player
- Thrown bombs stick to surfaces before exploding

### Mega Yoink

- Larger radius and higher cost
- All resulting drops are teleported to the triggering player

### Silk Yoink

- Block drops itself where supported
- All resulting drops are teleported to the triggering player

---

## Recipes

All recipes use the **Workbench** with category **Workbench_Tools**. Ingredients use Hytale IDs (e.g. `Ingredient_Powder_Boom`, `Ingredient_Fabric_Scrap_Linen`, `Ingredient_Bar_Iron`).

### Bombs

| Item | Ingredients | Output | Time |
|------|-------------|--------|------|
| **Area Bomb** | 4× Powder Boom, 3× Fabric Scrap Linen, 1× Iron Bar | 2 | 0.5 s |
| **Ore Bomb** | 5× Powder Boom, 3× Fabric Scrap Linen, 2× Iron Bar | 2 | 0.75 s |
| **Harvester Bomb** | 5× Powder Boom, 3× Fabric Scrap Linen, 1× Iron Bar | 2 | 0.75 s |
| **Mega Yoink** | 8× Powder Boom, 4× Fabric Scrap Linen, 3× Iron Bar | 1 | 1.0 s |
| **Silk Yoink** | 6× Powder Boom, 4× Fabric Scrap Linen, 2× Iron Bar | 1 | 1.0 s |

### Arrows (all five types)

Same recipe for **Area**, **Ore**, **Harvester**, **Mega Yoink**, and **Silk Yoink** arrows:

| Ingredients | Output | Time |
|-------------|--------|------|
| 1× Iron Bar, 1× Powder Boom, 1× Fabric Scrap Linen | 8 arrows | 0.5 s |

### Bow and crossbow

| Item | Ingredients | Output | Time |
|------|-------------|--------|------|
| **Yoink Bomb Bow** | 2× Iron Bar, 3× Fabric Scrap Linen, 2× Powder Boom | 1 | 1.0 s |
| **Yoink Bomb Crossbow** | 3× Iron Bar, 2× Fabric Scrap Linen, 3× Powder Boom | 1 | 1.5 s |

---

## Commands

Requires permission **yoinkbombs.admin** (e.g. OP). Values are saved to config.

| Command | Description |
|---------|-------------|
| `/yoinkbombs` | Show usage and available variants/attrs. |
| `/yoinkbombs reload` | Reload config from disk (if supported). |

### Bomb radius and damage

`/yoinkbombs <variant> <attr> <number>`

- **Variants:** `area`, `ore`, `harvester`, `mega`, `silk`
- **Attrs:** `BlockDamageRadius`, `EntityDamageRadius`
- **Number:** 0.5 up to variant max (e.g. 64 for area, 128 for mega, 32 for ore).

Examples:

- `/yoinkbombs area BlockDamageRadius 6`
- `/yoinkbombs mega EntityDamageRadius 12`

### Pull radii

`/yoinkbombs pull <attr> <number>`

- **ItemPullRadius** – Radius (blocks) for yoinking items at explosion position (default 12). Range: 1–64.
- **CrossbowPullRadius** – Radius for fallback pull when bow or crossbow is equipped (default 32). Range: 1–64.

Examples:

- `/yoinkbombs pull ItemPullRadius 24`
- `/yoinkbombs pull CrossbowPullRadius 48`

### Permissions

- **yoinkbombs.admin** – Use `/yoinkbombs` to change config and reload.
- **yoinkbombs.trusted** – Use Yoink Bombs block-break (break blocks with bomb in hand to trigger yoink).

---

## Ownership Rules

Yoink drops are delivered to the player who triggers the block break or who owns the projectile (thrown/shot). Fallback pull applies to the player holding the bow or crossbow.

---

## Protection and Safety

Yoink Bombs never bypass protection systems.

Supported safeguards:

- Claim / region protection respected

This mod is safe for:

- Survival servers
- Paid / whitelist servers
- Long-running worlds

---

## Protection Mod Support

Yoink Bombs integrates with protection plugins when available.

Supported:

- WorldProtect (auto-detected). Bomb block breaking is blocked in protected regions.

---

## Configuration

Config keys (file or via `/yoinkbombs` commands):

- **Explosion radii:** `AreaRadius`, `SilkRadius`, `MegaYoinkRadius`, `OreRadius`, `HarvesterRadius` (defaults: Area/Silk/Harvester 4, Mega 8, Ore 6).
- **Entity damage radii:** `AreaEntityDamageRadius`, `SilkEntityDamageRadius`, etc. (same defaults).
- **Pull:** `ItemPullRadius` (yoink at explosion, default 12, 1–64), `CrossbowPullRadius` (fallback when bow/crossbow equipped, default 32, 1–64).
- **Lists:** `BlockWhitelist`, `HarvesterWhitelist` (comma-separated).
- **Yoink:** `YoinkToInventory=true` – try inventory first, then spawn at feet.

---

## How Many Blocks Are Impacted?

The explosion uses a spherical radius. The approximate maximum number of blocks inside a radius is:

`blocks ≈ (4/3) * π * r^3`

This is an estimate. The real number can be slightly lower due to grid layout and filters.

Approximate blocks by radius (1 to 20):

| Radius | Approx Blocks |
| --- | --- |
| 1 | 4 |
| 2 | 34 |
| 3 | 113 |
| 4 | 268 |
| 5 | 524 |
| 6 | 905 |
| 7 | 1437 |
| 8 | 2145 |
| 9 | 3054 |
| 10 | 4189 |
| 11 | 5575 |
| 12 | 7238 |
| 13 | 9204 |
| 14 | 11494 |
| 15 | 14137 |
| 16 | 17157 |
| 17 | 20574 |
| 18 | 24429 |
| 19 | 28758 |
| 20 | 33510 |

---

## Edge Case Handling

- Inventory full: items spawn at player feet

---

## Compatibility

- No world format changes
- Safe to add or remove from existing worlds

---

## Use Cases

- Mining for ops and trusted players
- Clearing stone, dirt, or ore veins during mining

---

## Getting Started

- Configure `.env` with `HYTALE_HOME` (and optionally `JDK_URL`).
- Run `.\downloadJdk.ps1` if you use the repo-local JDK.
- Build: `.\gradlew.bat shadowJar`
- Start server: `.\gradlew.bat runServer`
- Prepare only: `.\gradlew.bat prepareServer`
- Stop server: `.\gradlew.bat stopServer` (uses `run\server.pid`)
- Drop extra test mods into `tests/mods`

Server package zip (optional):

- Place the latest server zip in `%USERPROFILE%\.hytale\server\` (example: `2026.01.28-87d03be09.zip`).
- The runner will extract the newest zip and use it instead of downloading.

