package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * A yoink to run at an explosion position (not at the player). Processed next tick so
 * block drops from Explode have spawned.
 */
public final class PendingYoink {

    private final Vector3d explosionPosition;
    private final Ref<EntityStore> ownerRef;
    private final long expiryTimeMillis;

    public PendingYoink(@javax.annotation.Nonnull Vector3d explosionPosition,
                        @javax.annotation.Nonnull Ref<EntityStore> ownerRef,
                        long expiryTimeMillis) {
        this.explosionPosition = explosionPosition.clone();
        this.ownerRef = ownerRef;
        this.expiryTimeMillis = expiryTimeMillis;
    }

    public Vector3d getExplosionPosition() {
        return explosionPosition;
    }

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
