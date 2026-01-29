package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Pending harvester-only block break at an explosion position. Processed next tick so only
 * crops/plants/trees (per harvester whitelist) are broken and drops are yoinked.
 */
public final class PendingHarvesterBreak {

    private final Vector3d explosionPosition;
    private final Ref<EntityStore> ownerRef;
    private final long expiryTimeMillis;

    public PendingHarvesterBreak(@Nonnull Vector3d explosionPosition,
                                  @Nonnull Ref<EntityStore> ownerRef,
                                  long expiryTimeMillis) {
        this.explosionPosition = explosionPosition.clone();
        this.ownerRef = ownerRef;
        this.expiryTimeMillis = expiryTimeMillis;
    }

    @Nonnull
    public Vector3d getExplosionPosition() {
        return explosionPosition;
    }

    @Nonnull
    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public long getExpiryTimeMillis() {
        return expiryTimeMillis;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiryTimeMillis;
    }
}
