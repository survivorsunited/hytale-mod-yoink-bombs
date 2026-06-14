package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * When a projectile is removed (bomb/arrow hit or miss), if the owner has a yoink item in hand,
 * records a pending yoink at the projectile position so items at the explosion site are yoinked
 * to the player.
 * <p>
 * Uses {@link EntityRemoveEvent#getEntity()} and the legacy {@link Entity} API (ref, world)
 * plus {@link ProjectileComponent#getCreatorUuid()} to resolve the owner. Only entities with the
 * <b>legacy</b> {@link ProjectileComponent} (entity.entities) are handled; if the game uses the
 * ECS projectile system (ProjectileModule) without that component, yoink will not trigger for
 * those projectiles.
 * <p>
 * Entity position is obtained from {@code ProjectileComponent.lastBouncePosition} (the impact
 * point) via reflection; if unavailable the entity's private {@code transformComponent} field is
 * read reflectively. This deliberately avoids {@code Entity.getTransformComponent()}, which is
 * absent from the runtime server API and would otherwise cause a {@link NoSuchMethodError}.
 */
public final class YoinkBombsExplosionListener {

    private static final String YOINK_PREFIX = "SU_YoinkBombs_";
    private static final long YOINK_DEFER_MS = 200L;

    private final Config<YoinkBombsConfig> config;
    private final List<PendingYoink> pendingYoinks;
    private final List<PendingHarvesterBreak> pendingHarvesterBreaks;

    public YoinkBombsExplosionListener(@Nonnull Config<YoinkBombsConfig> config,
                                       @Nonnull List<PendingYoink> pendingYoinks,
                                       @Nonnull List<PendingHarvesterBreak> pendingHarvesterBreaks) {
        this.config = config;
        this.pendingYoinks = pendingYoinks;
        this.pendingHarvesterBreaks = pendingHarvesterBreaks;
    }

    public void onEntityRemove(@Nonnull Object event) {
        if (!(event instanceof EntityRemoveEvent)) {
            return;
        }
        Entity entity = ((EntityRemoveEvent) event).getEntity();
        if (entity == null) {
            return;
        }
        Ref<EntityStore> entityRef = entity.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        if (entity.getWorld() == null) {
            return;
        }
        EntityStore entityStore = entity.getWorld().getEntityStore();
        if (entityStore == null) {
            return;
        }
        Store<EntityStore> store = entityStore.getStore();
        if (store == null) {
            return;
        }

        // Check for the projectile component BEFORE reading any position data.
        // This avoids calling Entity.getTransformComponent() on non-projectile entities;
        // that method does not exist in the runtime server API and would throw NoSuchMethodError.
        ProjectileComponent projectileComponent = store.getComponent(entityRef, ProjectileComponent.getComponentType());
        if (projectileComponent == null) {
            return;
        }

        java.util.UUID creatorUuid = getCreatorUuid(projectileComponent);
        if (creatorUuid == null) {
            return;
        }
        Ref<EntityStore> ownerRef = entity.getWorld().getEntityRef(creatorUuid);
        if (ownerRef == null || !ownerRef.isValid()) {
            return;
        }

        Player player = store.getComponent(ownerRef, Player.getComponentType());
        if (player == null || !hasYoinkInHand(player.getInventory())) {
            return;
        }

        // Obtain explosion position without calling Entity.getTransformComponent().
        // Primary source: lastBouncePosition on the ProjectileComponent (the actual impact point).
        // Fallback: the entity's private transformComponent field, accessed reflectively.
        Vector3d explosionPos = getExplosionPosition(projectileComponent, entity);
        if (explosionPos == null) {
            return;
        }

        long expiry = System.currentTimeMillis() + YOINK_DEFER_MS;
        pendingYoinks.add(new PendingYoink(explosionPos, ownerRef, expiry));
        if (isHarvesterProjectile(projectileComponent) || hasHarvesterInHand(player.getInventory())) {
            pendingHarvesterBreaks.add(new PendingHarvesterBreak(explosionPos, ownerRef, expiry));
        }
    }

    /**
     * Returns the explosion/impact position for the given projectile entity.
     *
     * <p>Prefers {@code ProjectileComponent.lastBouncePosition} (the point where the projectile
     * last bounced or hit), because it represents the actual explosion site and avoids touching
     * the entity transform altogether. Falls back to reading the entity's private
     * {@code transformComponent} field reflectively – this bypasses {@code getTransformComponent()},
     * which is absent from the runtime server API and causes a {@link NoSuchMethodError} when
     * called directly.
     */
    @Nullable
    private static Vector3d getExplosionPosition(@Nonnull ProjectileComponent projectileComponent,
                                                  @Nonnull Entity entity) {
        Vector3d lastBounce = getLastBouncePosition(projectileComponent);
        if (lastBounce != null) {
            return lastBounce.clone();
        }
        // Reflective fallback: read transformComponent field directly on Entity to avoid
        // the missing Entity.getTransformComponent() method at runtime.
        try {
            Field tcField = Entity.class.getDeclaredField("transformComponent");
            tcField.setAccessible(true);
            Object tc = tcField.get(entity);
            if (tc != null) {
                Method getPos = tc.getClass().getMethod("getPosition");
                Object pos = getPos.invoke(tc);
                if (pos instanceof Vector3d) {
                    return ((Vector3d) pos).clone();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Gets the last bounce/impact position from the ProjectileComponent via reflection.
     * The {@code lastBouncePosition} field is private and has no public accessor.
     */
    @Nullable
    private static Vector3d getLastBouncePosition(@Nonnull ProjectileComponent projectileComponent) {
        try {
            Field field = ProjectileComponent.class.getDeclaredField("lastBouncePosition");
            field.setAccessible(true);
            Object value = field.get(projectileComponent);
            return value instanceof Vector3d ? (Vector3d) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Tries to detect if the removed projectile was a harvester bomb/arrow via reflection on
     * ProjectileComponent (e.g. configId field). Fallback: owner has harvester item in hand.
     */
    private boolean isHarvesterProjectile(@Nonnull ProjectileComponent projectileComponent) {
        String configId = getProjectileConfigId(projectileComponent);
        return configId != null && configId.toLowerCase().contains("harvester");
    }

    @Nullable
    private static String getProjectileConfigId(@Nonnull ProjectileComponent projectileComponent) {
        for (String fieldName : new String[]{"configId", "projectileConfigId", "config"}) {
            try {
                Field field = ProjectileComponent.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(projectileComponent);
                if (value instanceof String) {
                    return (String) value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Gets creator UUID from ProjectileComponent. Uses reflection because the field may not be
     * exposed by a public getter in all server JAR versions.
     */
    @Nullable
    private static java.util.UUID getCreatorUuid(@Nonnull ProjectileComponent projectileComponent) {
        try {
            Field field = ProjectileComponent.class.getDeclaredField("creatorUuid");
            field.setAccessible(true);
            Object value = field.get(projectileComponent);
            return value instanceof java.util.UUID ? (java.util.UUID) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasYoinkInHand(@Nullable Inventory inventory) {
        if (inventory == null) return false;
        ItemStack hotbar = inventory.getActiveHotbarItem();
        ItemStack utility = inventory.getUtilityItem();
        if (hotbar != null && !hotbar.isEmpty() && hotbar.getItemId() != null && hotbar.getItemId().startsWith(YOINK_PREFIX))
            return true;
        if (utility != null && !utility.isEmpty() && utility.getItemId() != null && utility.getItemId().startsWith(YOINK_PREFIX))
            return true;
        return false;
    }

    private boolean hasHarvesterInHand(@Nullable Inventory inventory) {
        if (inventory == null) return false;
        String hotbarId = getItemId(inventory.getActiveHotbarItem());
        String utilityId = getItemId(inventory.getUtilityItem());
        return isHarvesterItem(hotbarId) || isHarvesterItem(utilityId);
    }

    @Nullable
    private static String getItemId(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() ? stack.getItemId() : null;
    }

    private static boolean isHarvesterItem(@Nullable String itemId) {
        if (itemId == null) return false;
        String lower = itemId.toLowerCase();
        return lower.contains("harvesterbomb") || lower.contains("harvesterarrow");
    }
}
