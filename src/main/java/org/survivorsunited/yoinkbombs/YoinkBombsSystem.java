package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Handles bomb behavior triggered by block breaks.
 */
public class YoinkBombsSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public static final String AREA_BOMB_ITEM_ID = "SU_YoinkBombs_AreaBomb";
    public static final String ORE_BOMB_ITEM_ID = "SU_YoinkBombs_OreBomb";
    public static final String HARVESTER_BOMB_ITEM_ID = "SU_YoinkBombs_HarvesterBomb";
    public static final String MEGA_YOINK_ITEM_ID = "SU_YoinkBombs_MegaYoink";
    public static final String SILK_YOINK_ITEM_ID = "SU_YoinkBombs_SilkYoink";
    public static final String TRUSTED_PERMISSION = "yoinkbombs.trusted";

    private final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);
    private final Config<YoinkBombsConfig> config;
    private final Object regionService;

    public YoinkBombsSystem(@Nonnull Config<YoinkBombsConfig> config) {
        super(BreakBlockEvent.class);
        this.config = config;
        this.regionService = null;
    }

    public YoinkBombsSystem(@Nonnull Config<YoinkBombsConfig> config, @Nullable Object regionService) {
        super(BreakBlockEvent.class);
        this.config = config;
        this.regionService = regionService;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        if (Boolean.TRUE.equals(this.isProcessing.get())) {
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null || itemInHand.isEmpty() || itemInHand.getItemId() == null) {
            return;
        }

        BombVariant variant = getVariant(itemInHand.getItemId());
        if (variant == null) {
            return;
        }

        BlockType targetType = event.getBlockType();
        if (targetType == null || isEmptyBlock(targetType)) {
            return;
        }

        Holder<EntityStore> holder = EntityUtils.toHolder(index, archetypeChunk);
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }
        // Only trusted players can use Yoink Bombs.
        if (!player.hasPermission(TRUSTED_PERMISSION)) {
            return;
        }

        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        Vector3i targetPos = event.getTargetBlock();
        World world = ((EntityStore) store.getExternalData()).getWorld();
        YoinkBombsConfig cfg = this.config.get();

        if (!canBuild(player, world, targetPos)) {
            return;
        }

        if (variant == BombVariant.ORE && !isOreBlock(targetType)) {
            return;
        }
        if (variant == BombVariant.HARVESTER && !isHarvestableCrop(targetType, cfg)) {
            return;
        }

        List<Vector3i> blocksToBreak = collectTargets(world, targetPos, targetType, variant, cfg);
        if (blocksToBreak.isEmpty()) {
            return;
        }

        this.isProcessing.set(true);
        try {
            event.setCancelled(true);
            processBlocks(world, blocksToBreak, playerRef, player, targetPos, store, commandBuffer, variant, cfg);
            consumeItemInHand(player.getInventory());
        } finally {
            this.isProcessing.set(false);
        }
    }

    @Nullable
    private BombVariant getVariant(@Nullable String itemId) {
        if (itemId == null) {
            return null;
        }
        String normalized = itemId.toLowerCase();
        if (normalized.contains("areabomb") || normalized.contains("areaarrow")) {
            return BombVariant.AREA;
        }
        if (normalized.contains("orebomb") || normalized.contains("orearrow")) {
            return BombVariant.ORE;
        }
        if (normalized.contains("harvesterbomb") || normalized.contains("harvesterarrow")) {
            return BombVariant.HARVESTER;
        }
        if (normalized.contains("megayoink")) {
            return BombVariant.MEGA_YOINK;
        }
        if (normalized.contains("silkyoink")) {
            return BombVariant.SILK_YOINK;
        }
        return null;
    }

    private List<Vector3i> collectTargets(@Nonnull World world, @Nonnull Vector3i targetPos, @Nonnull BlockType targetType,
                                          @Nonnull BombVariant variant, @Nonnull YoinkBombsConfig cfg) {
        if (variant == BombVariant.ORE) {
            return collectConnectedOres(world, targetPos, targetType.getId(), cfg.oreRadius());
        }

        double radius = cfg.areaRadius();
        if (variant == BombVariant.HARVESTER) {
            radius = cfg.harvesterRadius();
        } else if (variant == BombVariant.MEGA_YOINK) {
            radius = cfg.megaYoinkRadius();
        } else if (variant == BombVariant.SILK_YOINK) {
            radius = cfg.silkRadius();
        }

        int radiusCeil = (int) Math.ceil(radius);
        double radiusSq = radius * radius;
        List<Vector3i> results = new ArrayList<>();

        for (int dx = -radiusCeil; dx <= radiusCeil; dx++) {
            for (int dy = -radiusCeil; dy <= radiusCeil; dy++) {
                for (int dz = -radiusCeil; dz <= radiusCeil; dz++) {
                    double distSq = (dx * dx) + (dy * dy) + (dz * dz);
                    if (distSq > radiusSq) {
                        continue;
                    }
                    Vector3i pos = new Vector3i(targetPos.x + dx, targetPos.y + dy, targetPos.z + dz);
                    BlockType type = world.getBlockType(pos.x, pos.y, pos.z);
                    if (type == null || isEmptyBlock(type)) {
                        continue;
                    }
                    if (variant == BombVariant.HARVESTER && !isHarvestableCrop(type, cfg)) {
                        continue;
                    }
                    if (variant == BombVariant.ORE && !isOreBlock(type)) {
                        continue;
                    }
                    if (variant != BombVariant.AREA && variant != BombVariant.MEGA_YOINK && variant != BombVariant.SILK_YOINK
                            && !cfg.isBlockWhitelisted(type.getId())) {
                        continue;
                    }
                    results.add(pos);
                }
            }
        }

        return results;
    }

    private List<Vector3i> collectConnectedOres(@Nonnull World world, @Nonnull Vector3i startPos, @Nullable String targetId,
                                                double radius) {
        if (targetId == null) {
            return List.of();
        }

        int radiusCeil = (int) Math.ceil(radius);
        int radiusSq = radiusCeil * radiusCeil;
        Queue<Vector3i> queue = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();
        List<Vector3i> blocksToBreak = new ArrayList<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty()) {
            Vector3i current = queue.poll();
            BlockType currentBlock = world.getBlockType(current.x, current.y, current.z);
            if (currentBlock == null || !targetId.equals(currentBlock.getId())) {
                continue;
            }
            blocksToBreak.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Vector3i neighbor = new Vector3i(current.x + dx, current.y + dy, current.z + dz);
                        int distSq = (neighbor.x - startPos.x) * (neighbor.x - startPos.x)
                                + (neighbor.y - startPos.y) * (neighbor.y - startPos.y)
                                + (neighbor.z - startPos.z) * (neighbor.z - startPos.z);
                        if (distSq > radiusSq || visited.contains(neighbor)) {
                            continue;
                        }
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return blocksToBreak;
    }

    private void processBlocks(@Nonnull World world, @Nonnull List<Vector3i> blocksToBreak, @Nonnull Ref<EntityStore> playerRef,
                               @Nonnull Player player, @Nonnull Vector3i fallbackPos, @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BombVariant variant, @Nonnull YoinkBombsConfig cfg) {
        boolean yoink = true;
        boolean silk = variant == BombVariant.SILK_YOINK;
        Vector3d playerPos = getPlayerDropPosition(playerRef, fallbackPos, commandBuffer);

        for (Vector3i pos : blocksToBreak) {
            if (!canBuild(player, world, pos)) {
                continue;
            }
            BlockType type = world.getBlockType(pos.x, pos.y, pos.z);
            if (type == null || isEmptyBlock(type)) {
                continue;
            }

            List<ItemStack> drops = getBlockDrops(type, silk);
            for (ItemStack drop : drops) {
                if (yoink && cfg.yoinkToInventory() && tryAddToInventory(player.getInventory(), drop)) {
                    continue;
                }
                spawnItemDrop(store, commandBuffer, drop, playerPos);
            }

            world.setBlock(pos.x, pos.y, pos.z, "Empty", 256);
        }
    }

    private Vector3d getPlayerDropPosition(@Nonnull Ref<EntityStore> playerRef, @Nonnull Vector3i fallbackPos,
                                           @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        TransformComponent transform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        ModelComponent model = commandBuffer.getComponent(playerRef, ModelComponent.getComponentType());
        if (transform == null || model == null) {
            return new Vector3d(fallbackPos.x + 0.5D, fallbackPos.y + 0.5D, fallbackPos.z + 0.5D);
        }
        return transform.getPosition().clone().add(0.0D, model.getModel().getEyeHeight(), 0.0D);
    }

    private void spawnItemDrop(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                               @Nonnull ItemStack drop, @Nonnull Vector3d position) {
        Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(store, drop, position, Vector3f.ZERO, 0.0F, 0.15F, 0.0F);
        if (itemHolder != null) {
            commandBuffer.addEntity(itemHolder, AddReason.SPAWN);
        }
    }

    private boolean tryAddToInventory(@Nullable Inventory inventory, @Nonnull ItemStack drop) {
        if (inventory == null || drop.isEmpty()) {
            return false;
        }
        CombinedItemContainer container = inventory.getCombinedHotbarFirst();
        ItemStackTransaction transaction = container.addItemStack(drop);
        ItemStack remainder = transaction.getRemainder();
        return remainder == null || remainder.isEmpty() || remainder.getQuantity() <= 0;
    }

    private void consumeItemInHand(@Nullable Inventory inventory) {
        if (inventory == null) {
            return;
        }
        byte slot = inventory.getActiveHotbarSlot();
        if (slot < 0) {
            return;
        }
        inventory.getHotbar().removeItemStackFromSlot((short) slot, 1);
    }

    /**
     * Ore Bomb/Arrow (block-break path): only affect blocks whose ID starts with "Ore_" (e.g. Ore_Iron, Ore_Copper).
     * Thrown/shot Ore uses Explode JSON ItemTool GatherTypes (OreCopper, OreIron, ...) – engine restricts by gather type.
     */
    private boolean isOreBlock(@Nonnull BlockType blockType) {
        String id = blockType.getId();
        return id != null && id.toLowerCase().startsWith("ore_");
    }

    /**
     * Harvester Bomb/Arrow (block-break path): only affect blocks whose ID starts with one of the harvester
     * whitelist prefixes (default Plant_.* and Wood_.*). Thrown/shot Harvester uses Explode JSON ItemTool
     * GatherTypes (SoftBlocks, Woods) – engine restricts by gather type.
     */
    private boolean isHarvestableCrop(@Nonnull BlockType blockType, @Nonnull YoinkBombsConfig cfg) {
        String id = blockType.getId();
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase();
        List<String> entries = cfg.getHarvesterWhitelistEntries();
        if (entries.isEmpty()) {
            return false;
        }
        for (String entry : entries) {
            if (normalized.startsWith(entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyBlock(@Nonnull BlockType blockType) {
        String id = blockType.getId();
        return id == null || id.equals("Empty") || id.equalsIgnoreCase("empty");
    }

    private List<ItemStack> getBlockDrops(@Nonnull BlockType type, boolean silk) {
        List<ItemStack> drops = new ArrayList<>();
        if (silk) {
            String itemId = type.getItem() != null ? type.getItem().getId() : type.getId();
            drops.add(new ItemStack(itemId, 1));
            return drops;
        }

        BlockGathering gathering = type.getGathering();
        if (gathering != null && gathering.getBreaking() != null) {
            BlockBreakingDropType breaking = gathering.getBreaking();
            if (breaking.getDropListId() != null) {
                drops.addAll(ItemModule.get().getRandomItemDrops(breaking.getDropListId()));
            } else if (breaking.getItemId() != null) {
                drops.add(new ItemStack(breaking.getItemId(), breaking.getQuantity()));
            }
        }

        if (drops.isEmpty()) {
            String fallbackId = type.getItem() != null ? type.getItem().getId() : type.getId();
            drops.add(new ItemStack(fallbackId, 1));
        }

        return drops;
    }

    private boolean canBuild(@Nonnull Player player, @Nonnull World world, @Nonnull Vector3i pos) {
        if (this.regionService == null) {
            return true;
        }
        try {
            Object playerRef = player.getPlayerRef();
            Class<?> playerRefClass = Class.forName("com.hypixel.hytale.server.core.universe.PlayerRef");
            java.lang.reflect.Method method = this.regionService.getClass().getMethod("canBuild", Player.class, playerRefClass, World.class, Vector3i.class);
            Object result = method.invoke(this.regionService, player, playerRef, world, pos);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private enum BombVariant {
        AREA,
        ORE,
        HARVESTER,
        MEGA_YOINK,
        SILK_YOINK
    }
}
