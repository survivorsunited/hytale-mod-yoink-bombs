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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * When a projectile is removed (bomb/arrow hit or miss), if the owner has a yoink item in hand,
 * records a pending yoink at the projectile position so items at the explosion site are yoinked
 * to the player.
 * <p>
 * Uses {@link EntityRemoveEvent#getEntity()} and the legacy {@link Entity} API (ref, transform, world)
 * plus {@link ProjectileComponent#getCreatorUuid()} to resolve the owner. Only entities with the
 * <b>legacy</b> {@link ProjectileComponent} (entity.entities) are handled; if the game uses the
 * ECS projectile system (ProjectileModule) without that component, yoink will not trigger for
 * those projectiles.
 */
public final class YoinkBombsExplosionListener {

    private static final String YOINK_PREFIX = "SU_YoinkBombs_";
    private static final long YOINK_DEFER_MS = 200L;

    private final Config<YoinkBombsConfig> config;
    private final List<PendingYoink> pendingYoinks;

    public YoinkBombsExplosionListener(@Nonnull Config<YoinkBombsConfig> config,
                                       @Nonnull List<PendingYoink> pendingYoinks) {
        this.config = config;
        this.pendingYoinks = pendingYoinks;
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

        @SuppressWarnings("removal") // Entity.getTransformComponent() deprecated; no replacement in server API
        TransformComponent transform = entity.getTransformComponent();
        if (transform == null) {
            return;
        }
        Vector3d explosionPos = transform.getPosition().clone();

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

        long expiry = System.currentTimeMillis() + YOINK_DEFER_MS;
        pendingYoinks.add(new PendingYoink(explosionPos, ownerRef, expiry));
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
}
