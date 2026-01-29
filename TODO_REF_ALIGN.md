# TODO: Align Yoink Bombs with JCP.ExplosivesPack Reference

Reference mod: `refs/JCP.ExplosivesPack-1.0.6`.  
Goal: Bombs behave like **C4/Adhesive Bomb**; arrows behave like **Explosive Arrow** (no custom skins/colours).

---

## 1. Bombs (throwables) – match C4 / Adhesive Bomb

### 1.1 Item interaction flow (C4 pattern)
- **Ref**: Adhesive Bomb uses **Secondary** (not Primary).  
  `Interactions.Secondary` = array: `["Adhesive_Bomb_Throw", { "Type": "ModifyInventory", "ItemToRemove": { "Id": "Adhesive_Bomb" } }]`.
- **TODO**: Decide if our bombs stay Primary or switch to Secondary; ensure **one** throw interaction + **one** ModifyInventory consume, matching ref structure.

### 1.2 Throw interaction (C4 pattern)
- **Ref**: `Adhesive_Bomb_Throw.json` – Type **Serial**, order: (1) **Simple** RunTime 0.25, Effects ItemAnimationId "Throw", (2) **Projectile** Config `Projectile_Config_Adhesive_Bomb`.
- **TODO**: For each bomb variant, use **Serial**: Simple (Throw animation) → Projectile (our projectile config). No branching; same pattern as ref.

### 1.3 Projectile config (C4 pattern)
- **Ref**: `Projectile_Config_Adhesive_Bomb.json`:
  - **No Parent** (standalone).
  - **Model**: `Adhesive_Bomb_Projectile` (separate model asset pointing at C4 model/texture).
  - **Physics**: Standard, Gravity 20, Bounciness 0, **SticksVertically: true**.
  - **LaunchForce**: 25, **SpawnOffset**: Y 0.05, Z -1.
  - **ProjectileHit**: Cooldown 0 → **Simple** RunTime 0 (sound C4_Place) → **RemoveEntity** Entity "User" → **Adhesive_Bomb_Explode**.
  - **ProjectileMiss**: Cooldown 0 → Simple RunTime **3** (sound C4_Place) → RemoveEntity → Adhesive_Bomb_Explode.
- **TODO**: Align our bomb projectile configs with this order and structure. Use **base-game bomb model/sound** (e.g. Goblin bomb) or ref C4 if we bundle it; no Rules.Interrupting unless we prove we need it.

### 1.4 Explode interaction (C4 pattern)
- **Ref**: `Adhesive_Bomb_Explode.json` – Type Explode, Config (DamageEntities/Blocks, radii, BlockDropChance, EntityDamage, ItemTool.Specs), Effects (Particles, WorldSoundEventId C4_Explode).
- **TODO**: Keep our explode JSONs; ensure they are **only** referenced from projectile hit/miss (no duplicate logic). Use base-game or ref sound IDs that exist.

### 1.5 Projectile model asset (C4 pattern)
- **Ref**: `Adhesive_Bomb_Projectile` model JSON references Items/Weapons/AdhesiveBombs/C4.model and texture; has HitBox, optional Trails, AnimationSets Idle (spin).
- **TODO**: Our projectile configs reference a model (e.g. Bomb_Fire_Goblin or parent). Confirm that model asset exists in base game or our pack; if not, add a minimal projectile model asset or use Parent from base.

---

## 2. Arrows (bow/crossbow) – match Explosive Arrow

### 2.1 How ref crossbow works (no custom root)
- **Ref**: Explosive **Crossbow** item:
  - **Interactions.Primary** = `Root_Weapon_Crossbow_Primary_Signature` (vanilla root).
  - **InteractionVars** on the **item** define: Arrow_Inventory_Condition (consume Weapon_Arrow_Explosive), Standard_Projectile_Launch (Weapon_Explosive_Crossbow_Shot_Standard_Projectile), **Standard_Projectile_Impact** = Explosive_Arrow_Explode, **Standard_Projectile_Miss** = Explosive_Arrow_Explode, No_Ammo_Effects, etc.
- **Note (verified in logs)**: InteractionVars `Standard_Projectile_Impact` / `Standard_Projectile_Miss` must reference **RootInteraction** asset IDs. Using a plain Interaction ID (e.g. `AreaBomb_Explode`) causes validation failure: "Asset 'AreaBomb_Explode' of type RootInteraction doesn't exist!" and the crossbow item is removed. To use Option A we would need RootInteraction wrappers for our explode interactions (or the ref pack’s explode is registered as a root).
- **Ref** projectile config `Projectile_Config_Explosive_Arrow_Crossbow` does **not** hardcode explode; it uses **Replace** Var `Standard_Projectile_Impact` / `Standard_Projectile_Miss` with DefaultValue pointing at vanilla impact/miss, and the **weapon item’s InteractionVars** override those to Explosive_Arrow_Explode.
- **TODO**: Either:
  - **Option A (crossbow)**: Give our crossbow item **vanilla crossbow root** (e.g. Root_Weapon_Crossbow_Primary_Signature) and define **InteractionVars** for ammo (our arrow IDs), Standard_Projectile_Launch (our projectile config), Standard_Projectile_Impact / Standard_Projectile_Miss (our explode interactions). One arrow type per weapon or priority list; may need one “explosive” arrow item that we use for all variants or one var set per variant.
  - **Option B**: Keep custom root + custom primary; then ensure the **primary** correctly uses base-game **charging** + ammo check + **projectile launch** so it’s a valid chain the engine accepts (reference Weapon_Explosive_Shortbow_Primary_Shoot_Charge and Weapon_Shortbow_Primary_Shoot_Strength_* for shortbow).

### 2.2 How ref shortbow works (custom charge + one arrow)
- **Ref**: Explosive **Shortbow** uses custom **Weapon_Explosive_Shortbow_Primary_Shoot_Charge** (Type Charging) with Next keys "0.1" … "1.2". Each step: ModifyInventory (Weapon_Arrow_Explosive), Next = Replace Var Primary_Shoot_Strength_N (DefaultValue Weapon_Shortbow_Primary_Shoot_Strength_N), Failed = Replace Var No_Ammo_Effects (DefaultValue Common_Bow_No_Ammo).
- **Ref** shoot strength interactions (e.g. Weapon_Shortbow_Primary_Shoot_Strength_A1) use **Parent** Weapon_Explosive_Shortbow_Primary_Shoot_Projectile and **Config** Projectile_Config_Explosive_Arrow_Shortbow_Strength_A1.
- **TODO**: If we keep a custom bow: implement a **charging** primary that consumes **one** of our arrow types (or a priority list), then Replace Var to a **shoot** interaction that launches our projectile config. Provide **No_Ammo_Effects** and all strength vars so the engine never looks for “replacement interactions” on another item (our item must define them).

### 2.3 Arrow projectile config (ref pattern)
- **Ref**: `Projectile_Config_Explosive_Arrow_Crossbow` – Parent Projectile_Config_Arrow_Base, LaunchForce 40, Physics (SticksVertically true), **Interactions**: ProjectileHit = Weapon_Crossbow_Primary_Combo_Condition + **Replace Var Standard_Projectile_Impact** (DefaultValue Weapon_Crossbow_Effects_Standard_Projectile_Impact) + Common_Projectile_Despawn; ProjectileMiss = Replace Var Standard_Projectile_Miss (DefaultValue Common_Projectile_Miss) + Common_Projectile_Despawn. **Model** = Explosive_Arrow_Projectile (custom). SpawnOffset / SpawnRotationOffset set.
- **TODO**: For **Option A**: Our projectile config should use **Replace** for impact/miss so the **weapon item’s InteractionVars** supply the explode (like ref). For **Option B**: We can keep direct explode in projectile if our weapon never uses vanilla vars. Prefer **Option A** so one crossbow item + InteractionVars drives behaviour.

### 2.4 Arrow item and visuals (no custom skins/colours)
- **Ref**: Weapon_Arrow_Explosive has its own model/texture/icon in the ref pack.
- **TODO**: Use **base-game arrow** model/texture/icon (e.g. iron arrow) for our arrow items; only change behaviour (projectile config / explode). No custom skins or colours.

### 2.5 Sound and model IDs
- **Ref**: Explosive_Arrow_Explode uses WorldSoundEventId **Arrow_Explode** (ref pack). C4 uses **C4_Place** / **C4_Explode** (ref pack).
- **TODO**: Use only **base-game** sound IDs for our mod (e.g. SFX_Goblin_Lobber_Bomb_Death) unless we bundle ref sounds; do not reference C4_Place / C4_Explode / Arrow_Explode if we don’t ship those assets.

### 2.6 Shortbow check (base game)
- **Path to check**: Base-game Shortbow interactions live under:
  - **`.hytale\Assets\Server\Item\Interactions\Weapons\Shortbow`** (if you have Assets extracted), or
  - Inside **Assets.zip** from Hytale install: `install\<patchline>\package\game\latest\Assets.zip` → extract and open `Server/Item/Interactions/Weapons/Shortbow`.
- **What to compare**:
  - Vanilla **root** for shortbow (e.g. RootInteraction that points at the charging primary).
  - Vanilla **Weapon_Shortbow_Primary_Shoot_Strength_0** … **Weapon_Shortbow_Primary_Shoot_Strength_4** (ref Charge uses these as DefaultValue for Replace Var `Primary_Shoot_Strength_0` … `Primary_Shoot_Strength_4`).
  - Whether **Weapon_Explosive_Shortbow_Primary_Shoot_Projectile** exists in base game (ref’s `Weapon_Shortbow_Primary_Shoot_Strength_A1.json` uses it as Parent; server log reported this parent missing when loading ref pack).
  - Vanilla charging primary structure (Type Charging, Next keys "0.1", "0.3", "0.6", "0.9", "1.2") so our bow can mirror it with our arrow types.
- **Ref pattern (from JCP.ExplosivesPack)**: `Weapon_Explosive_Shortbow_Primary_Shoot_Charge.json` – Type **Charging**, Next keys "0.1"|"0.3"|"0.6"|"0.9"|"1.2", each step: ModifyInventory (Weapon_Explosive_Arrow) → Next = Replace Var `Primary_Shoot_Strength_N` (DefaultValue `Weapon_Shortbow_Primary_Shoot_Strength_N`), Failed = Replace Var `No_Ammo_Effects` (DefaultValue Common_Bow_No_Ammo). Ref’s Strength override: `Weapon_Shortbow_Primary_Shoot_Strength_A1.json` has Parent `Weapon_Explosive_Shortbow_Primary_Shoot_Projectile`, Config `Projectile_Config_Explosive_Arrow_Shortbow_Strength_A1`.
- **Done (Option B)**: Our bow now uses custom **YoinkBombs_Bow_Primary_Shoot_Charge** (Type Charging, Next 0.1/0.3/0.6/0.9/1.2) consuming **SU_YoinkBombs_AreaArrow**, Replace Var Primary_Shoot_Strength_0..4 (DefaultValue YoinkBombs_Bow_Shoot_Area), Failed = Replace Var No_Ammo_Effects (Common_Bow_No_Ammo). Bow item has **InteractionVars** Primary_Shoot_Strength_0..4 and No_Ammo_Effects so the engine never falls back to vanilla. **Note**: Bow currently accepts only Area arrow for the Charging flow; to support Ore/Harvester/Mega/Silk again, extend Charging (e.g. multiple ammo checks) or add a separate interaction path.

---

## 3. Implementation order (suggested)

1. **Bombs**: Rewrite one bomb (e.g. Area) to follow C4 exactly (Secondary or Primary throw → Serial Throw + Projectile → projectile config with Simple + RemoveEntity + Explode, correct order). Test stick + explode. Then replicate for Ore, Harvester, Mega, Silk.
2. **Crossbow + one arrow**: Implement **Option A** – crossbow item with vanilla root + InteractionVars; one arrow item (e.g. “Area” or generic explosive); projectile config with Replace impact/miss; impact/miss vars = our explode. Test fire and explode.
3. **Bow**: Either same as crossbow (if base bow root exists and supports vars) or **Option B** – custom charging primary + No_Ammo_Effects and strength vars defined on our bow item; one arrow type; projectile config. Test fire and explode.
4. **Multiple arrow variants**: Extend InteractionVars or primary chain to support multiple arrow IDs (Area, Ore, Harvester, Mega, Silk) with correct projectile/explode per type; re-use base arrow visuals for all.
5. **Cleanup**: Remove any custom skins/colours; ensure all sound/model IDs exist in base or our pack; document required assets.

---

## 4. Reference file checklist

| Ref path | Purpose |
|----------|--------|
| Server/Item/Items/Weapon/Adhesive_Bomb.json | C4 item: Secondary = Throw + ModifyInventory |
| Server/Item/Interactions/Weapons/Adhesive_Bomb_Throw.json | Serial: Simple (Throw) → Projectile |
| Server/ProjectileConfigs/Weapons/Throwables/Projectile_Config_Adhesive_Bomb.json | Hit: Simple → RemoveEntity → Explode; Miss: Simple(3s) → RemoveEntity → Explode |
| Server/Item/Interactions/Adhesive_Bomb_Explode.json | Explode config + effects |
| Server/Models/Projectiles/Weapons/Bullet/Adhesive_Bomb_Projectile.json | C4 projectile model |
| Server/Item/Items/Weapon/Crossbow/Weapon_Explosive_Crossbow.json | Crossbow: vanilla root + InteractionVars (Impact/Miss = Explosive_Arrow_Explode) |
| Server/Item/Interactions/Weapons/Crossbow/Shots/Weapon_Explosive_Crossbow_Shot_Standard_Projectile.json | Projectile launch: Config Projectile_Config_Explosive_Arrow_Crossbow |
| Server/ProjectileConfigs/Weapons/Arrows/Projectile_Config_Explosive_Arrow_Crossbow.json | Replace Standard_Projectile_Impact/Miss (weapon vars supply explode) |
| Server/Item/Interactions/Explosive_Arrow_Explode.json | Arrow explode interaction |
| Server/Item/Interactions/Weapon_Explosive_Shortbow_Primary_Shoot_Charge.json | Bow: Charging + ModifyInventory + Replace strength + No_Ammo_Effects |
| Server/Item/Interactions/Weapons/Explosive_Shortbow/Weapon_Shortbow_Primary_Shoot_Strength_A1.json | Ref Strength override: Parent Weapon_Explosive_Shortbow_Primary_Shoot_Projectile, Config explosive arrow |
| Server/Item/RootInteractions/Weapons/Crossbow/Root_Weapon_Explosive_Crossbow_Swap_From.json | SwapFrom root for explosive crossbow |
| **Base game** `.hytale\Assets\Server\Item\Interactions\Weapons\Shortbow` (or Assets.zip) | Vanilla Weapon_Shortbow_Primary_Shoot_Strength_0..4, root, and optional Weapon_Explosive_Shortbow_Primary_Shoot_Projectile |

---

## 5. Yoink (Java) – unchanged for now

- Yoink is implemented in `YoinkBombsSystem` on **BreakBlockEvent**; it yoinks when the break is caused by our bomb/arrow (itemInHand or variant detection). No change to this TODO unless we find that explosion breaks don’t fire BreakBlockEvent or don’t carry the right item/context; then we add a separate task to trace and fix.
