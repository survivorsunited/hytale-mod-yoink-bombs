package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Processes pending yoinks at explosion position: when a bomb/arrow explodes, items at that
 * position are instantly transferred to the owner. When the YoinkBombs bow or crossbow is equipped,
 * also pulls items within a large radius (configurable, default 32 blocks) around the player.
 */
public class YoinkBombsItemPullSystem extends EntityTickingSystem<EntityStore> {

    private static final String YOINK_BOW_ITEM_ID = "SU_YoinkBombs_Bow";
    private static final String YOINK_CROSSBOW_ITEM_ID = "SU_YoinkBombs_Crossbow";

    @Nonnull
    private final Query<EntityStore> query = Query.and(Player.getComponentType());
    @Nonnull
    private final Config<YoinkBombsConfig> config;
    @Nonnull
    private final List<PendingYoink> pendingYoinks;

    public YoinkBombsItemPullSystem(@Nonnull Config<YoinkBombsConfig> config,
                                    @Nonnull List<PendingYoink> pendingYoinks) {
        this.config = config;
        this.pendingYoinks = pendingYoinks;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> playerRef = archetypeChunk.getReferenceTo(index);
        Player player = EntityUtils.toHolder(index, archetypeChunk).getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        double radius = this.config.get().itemPullRadius();
        CombinedItemContainer container = player.getInventory() != null
                ? player.getInventory().getCombinedHotbarFirst()
                : null;
        if (container == null) {
            removeExpired(now);
            return;
        }

        TransformComponent playerTransform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        ModelComponent modelComponent = commandBuffer.getComponent(playerRef, ModelComponent.getComponentType());
        Vector3d playerPos = (playerTransform != null && modelComponent != null)
                ? playerTransform.getPosition().clone().add(0.0D, modelComponent.getModel().getEyeHeight(), 0.0D)
                : null;

        Iterator<PendingYoink> it = pendingYoinks.iterator();
        while (it.hasNext()) {
            PendingYoink pending = it.next();
            if (pending.isExpired(now)) {
                it.remove();
                continue;
            }
            if (!pending.getOwnerRef().equals(playerRef) || !pending.getOwnerRef().isValid()) {
                continue;
            }
            it.remove();

            Vector3d explosionPos = pending.getExplosionPosition();
            List<Ref<EntityStore>> nearby = new ArrayList<>();
            SpatialResource<Ref<EntityStore>, EntityStore> itemSpatialResource =
                    commandBuffer.getResource(EntityModule.get().getItemSpatialResourceType());
            itemSpatialResource.getSpatialStructure().collect(explosionPos, radius, nearby);

            for (Ref<EntityStore> entityRef : nearby) {
                ItemComponent itemComponent = commandBuffer.getComponent(entityRef, ItemComponent.getComponentType());
                if (itemComponent == null || !itemComponent.canPickUp()) {
                    continue;
                }
                ItemStack stack = itemComponent.getItemStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                ItemStackTransaction transaction = container.addItemStack(stack);
                commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);

                ItemStack remainder = transaction.getRemainder();
                if (remainder != null && !remainder.isEmpty() && playerPos != null) {
                    spawnItemDrop(store, commandBuffer, remainder, playerPos);
                }
            }
        }

        /* Fallback: when YoinkBombs bow or crossbow is equipped, pull items within large radius (e.g. 32 blocks). */
        if (playerPos != null && hasYoinkRangedWeaponEquipped(player)) {
            double crossbowRadius = this.config.get().crossbowPullRadius();
            Vector3d feetPos = playerTransform != null ? playerTransform.getPosition().clone() : playerPos;
            List<Ref<EntityStore>> nearPlayer = new ArrayList<>();
            SpatialResource<Ref<EntityStore>, EntityStore> itemSpatial =
                    commandBuffer.getResource(EntityModule.get().getItemSpatialResourceType());
            itemSpatial.getSpatialStructure().collect(feetPos, crossbowRadius, nearPlayer);
            for (Ref<EntityStore> entityRef : nearPlayer) {
                ItemComponent itemComponent = commandBuffer.getComponent(entityRef, ItemComponent.getComponentType());
                if (itemComponent == null || !itemComponent.canPickUp()) {
                    continue;
                }
                ItemStack stack = itemComponent.getItemStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                ItemStackTransaction transaction = container.addItemStack(stack);
                commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
                ItemStack remainder = transaction.getRemainder();
                if (remainder != null && !remainder.isEmpty()) {
                    spawnItemDrop(store, commandBuffer, remainder, feetPos);
                }
            }
        }

        removeExpired(now);
    }

    /**
     * True if the player has the Yoink Bombs bow or crossbow in hand (hotbar or utility).
     * Used for fallback item pull so items are pulled when either ranged weapon is equipped.
     */
    private static boolean hasYoinkRangedWeaponEquipped(@Nonnull Player player) {
        if (player.getInventory() == null) {
            return false;
        }
        ItemStack hotbar = player.getInventory().getActiveHotbarItem();
        ItemStack utility = player.getInventory().getUtilityItem();
        if (hotbar != null && !hotbar.isEmpty()) {
            String id = hotbar.getItemId();
            if (YOINK_BOW_ITEM_ID.equals(id) || YOINK_CROSSBOW_ITEM_ID.equals(id)) {
                return true;
            }
        }
        if (utility != null && !utility.isEmpty()) {
            String id = utility.getItemId();
            if (YOINK_BOW_ITEM_ID.equals(id) || YOINK_CROSSBOW_ITEM_ID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void removeExpired(long now) {
        pendingYoinks.removeIf(p -> p.isExpired(now));
    }

    private void spawnItemDrop(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                               @Nonnull ItemStack drop, @Nonnull Vector3d position) {
        Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(
                store, drop, position, Vector3f.ZERO, 0.0F, 0.15F, 0.0F);
        if (itemHolder != null) {
            commandBuffer.addEntity(itemHolder, AddReason.SPAWN);
        }
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return EntityTickingSystem.maybeUseParallel(archetypeChunkSize, taskCount);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }
}
